/*
 * Copyright The OpenTelemetry Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.opentelemetry.instrumentation.auto.httpurlconnection;

import static io.opentelemetry.auto.tooling.bytebuddy.matcher.AgentElementMatchers.extendsClass;
import static io.opentelemetry.auto.tooling.matcher.NameMatchers.namedOneOf;
import static io.opentelemetry.instrumentation.auto.httpurlconnection.HttpUrlConnectionTracer.TRACER;
import static io.opentelemetry.trace.TracingContextUtils.currentContextWith;
import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;

import com.google.auto.service.AutoService;
import io.opentelemetry.auto.tooling.Instrumenter;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.auto.api.CallDepthThreadLocalMap;
import io.opentelemetry.instrumentation.auto.api.ContextStore;
import io.opentelemetry.instrumentation.auto.api.InstrumentationContext;
import io.opentelemetry.trace.Span;
import java.net.HttpURLConnection;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;

@AutoService(Instrumenter.class)
public class HttpUrlConnectionInstrumentation extends Instrumenter.Default {

  public HttpUrlConnectionInstrumentation() {
    super("httpurlconnection");
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return nameStartsWith("java.net.")
        .or(ElementMatchers.<TypeDescription>nameStartsWith("sun.net"))
        // This class is a simple delegator. Skip because it does not update its `connected` field.
        .and(not(named("sun.net.www.protocol.https.HttpsURLConnectionImpl")))
        .and(extendsClass(named("java.net.HttpURLConnection")));
  }

  @Override
  public String[] helperClassNames() {
    return new String[] {
      packageName + ".HttpUrlConnectionTracer",
      packageName + ".HeadersInjectAdapter",
      HttpUrlConnectionInstrumentation.class.getName() + "$HttpUrlState",
      HttpUrlConnectionInstrumentation.class.getName() + "$HttpUrlState$1",
    };
  }

  @Override
  public Map<String, String> contextStore() {
    return singletonMap("java.net.HttpURLConnection", getClass().getName() + "$HttpUrlState");
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    return singletonMap(
        isMethod().and(isPublic()).and(namedOneOf("connect", "getOutputStream", "getInputStream")),
        HttpUrlConnectionInstrumentation.class.getName() + "$HttpUrlConnectionAdvice");
  }

  public static class HttpUrlConnectionAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static HttpUrlState methodEnter(
        @Advice.This final HttpURLConnection thiz,
        @Advice.FieldValue("connected") final boolean connected,
        @Advice.Local("otelScope") Scope scope) {

      int callDepth = CallDepthThreadLocalMap.incrementCallDepth(HttpURLConnection.class);
      if (callDepth > 0) {
        return null;
      }

      ContextStore<HttpURLConnection, HttpUrlState> contextStore =
          InstrumentationContext.get(HttpURLConnection.class, HttpUrlState.class);
      HttpUrlState state = contextStore.putIfAbsent(thiz, HttpUrlState.FACTORY);

      synchronized (state) {
        if (!state.hasSpan() && !state.isFinished()) {
          Span span = state.start(thiz);
          if (!connected) {
            scope = TRACER.startScope(span, thiz);
          }
        }
        return state;
      }
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void methodExit(
        @Advice.Enter final HttpUrlState state,
        @Advice.FieldValue("responseCode") final int responseCode,
        @Advice.Thrown final Throwable throwable,
        @Advice.Origin("#m") final String methodName,
        @Advice.Local("otelScope") Scope scope) {

      if (scope != null) {
        scope.close();
      }
      if (state == null) {
        return;
      }
      CallDepthThreadLocalMap.reset(HttpURLConnection.class);

      synchronized (state) {
        if (state.hasSpan() && !state.isFinished()) {
          if (throwable != null) {
            state.finishSpan(throwable);
          } else if ("getInputStream".equals(methodName)) {
            state.finishSpan(responseCode);
          }
        }
      }
    }
  }

  public static class HttpUrlState {

    public static final ContextStore.Factory<HttpUrlState> FACTORY =
        new ContextStore.Factory<HttpUrlState>() {
          @Override
          public HttpUrlState create() {
            return new HttpUrlState();
          }
        };

    private volatile Span span = null;
    private volatile boolean finished = false;

    public Span start(final HttpURLConnection connection) {
      span = TRACER.startSpan(connection);
      return span;
    }

    public boolean hasSpan() {
      return span != null;
    }

    public boolean isFinished() {
      return finished;
    }

    public void finishSpan(final Throwable throwable) {
      try (Scope scope = currentContextWith(span)) {
        TRACER.endExceptionally(span, throwable);
        span = null;
        finished = true;
      }
    }

    public void finishSpan(final int responseCode) {
      /*
       * responseCode field is sometimes not populated.
       * We can't call getResponseCode() due to some unwanted side-effects
       * (e.g. breaks getOutputStream).
       */
      if (responseCode > 0) {
        try (Scope scope = currentContextWith(span)) {
          // Need to explicitly cast to boxed type to make sure correct method is called.
          // https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/946
          TRACER.end(span, (Integer) responseCode);
          span = null;
          finished = true;
        }
      }
    }
  }
}
