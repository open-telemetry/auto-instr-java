/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.v5_0;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.implementsInterface;
import static io.opentelemetry.javaagent.instrumentation.apachehttpclient.v5_0.ApacheHttpClientContextManager.httpContextManager;
import static io.opentelemetry.javaagent.instrumentation.apachehttpclient.v5_0.ApacheHttpClientInernalEntityStorage.storage;
import static net.bytebuddy.matcher.ElementMatchers.isAbstract;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.protocol.HttpContext;

/**
 * Apache HttpClient makes an internal copy of the request sent by the user and may not give actual
 * response in case of errors back to the user. It internally stores this information in it's http
 * context. Hence, to fetch the attributes we instrument the client interceptors.
 */
public final class ApacheHttpClientProcessorInstrumentation implements TypeInstrumentation {
  @Override
  public ElementMatcher<ClassLoader> classLoaderOptimization() {
    return hasClassesNamed("org.apache.hc.core5.http.protocol.HttpProcessor");
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return implementsInterface(named("org.apache.hc.core5.http.protocol.HttpProcessor"));
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod()
            .and(named("process"))
            .and(isPublic())
            .and(not(isAbstract()))
            .and(takesArguments(3))
            .and(takesArgument(0, named("org.apache.hc.core5.http.HttpRequest")))
            .and(takesArgument(1, named("org.apache.hc.core5.http.EntityDetails")))
            .and(takesArgument(2, named("org.apache.hc.core5.http.protocol.HttpContext"))),
        this.getClass().getName() + "$HttpRequestProcessorAdvice");

    transformer.applyAdviceToMethod(
        isMethod()
            .and(named("process"))
            .and(isPublic())
            .and(not(isAbstract()))
            .and(takesArguments(3))
            .and(takesArgument(0, named("org.apache.hc.core5.http.HttpResponse")))
            .and(takesArgument(1, named("org.apache.hc.core5.http.EntityDetails")))
            .and(takesArgument(2, named("org.apache.hc.core5.http.protocol.HttpContext"))),
        this.getClass().getName() + "$HttpResponseProcessorAdvice");
  }

  @SuppressWarnings("unused")
  public static class HttpRequestProcessorAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void methodEnter(
        @Advice.Argument(value = 0) HttpRequest httpRequest,
        @Advice.Argument(value = 1) EntityDetails entityDetails,
        @Advice.Argument(value = 2) HttpContext httpContext) {
      Context context = httpContextManager().getCurrentContext(httpContext);
      if (context != null) {
        storage().storeHttpRequest(context, httpRequest);
      }
    }
  }

  @SuppressWarnings("unused")
  public static class HttpResponseProcessorAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void methodEnter(
        @Advice.Argument(value = 0) HttpResponse httpResponse,
        @Advice.Argument(value = 1) EntityDetails entityDetails,
        @Advice.Argument(value = 2) HttpContext httpContext) {
      Context context = httpContextManager().getCurrentContext(httpContext);
      if (context != null) {
        storage().storeHttpResponse(context, httpResponse);
      }
    }
  }
}
