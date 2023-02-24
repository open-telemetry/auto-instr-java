/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.v4_0.commons;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.implementsInterface;
import static io.opentelemetry.javaagent.instrumentation.apachehttpclient.v4_0.commons.ApacheHttpClientContextManager.httpContextManager;
import static io.opentelemetry.javaagent.instrumentation.apachehttpclient.v4_0.commons.ApacheHttpClientInernalEntityStorage.storage;
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
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.HttpContext;

public final class ApacheHttpClientProcessorInstrumentation implements TypeInstrumentation {
  @Override
  public ElementMatcher<ClassLoader> classLoaderOptimization() {
    return hasClassesNamed("org.apache.http.protocol.HttpProcessor");
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return implementsInterface(named("org.apache.http.protocol.HttpProcessor"));
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod()
            .and(named("process"))
            .and(isPublic())
            .and(not(isAbstract()))
            .and(takesArguments(2))
            .and(takesArgument(0, named("org.apache.http.HttpRequest")))
            .and(takesArgument(1, named("org.apache.http.protocol.HttpContext"))),
        this.getClass().getName() + "$HttpRequestProcessorAdvice");

    transformer.applyAdviceToMethod(
        isMethod()
            .and(named("process"))
            .and(isPublic())
            .and(not(isAbstract()))
            .and(takesArguments(2))
            .and(takesArgument(0, named("org.apache.http.HttpResponse")))
            .and(takesArgument(1, named("org.apache.http.protocol.HttpContext"))),
        this.getClass().getName() + "$HttpResponseProcessorAdvice");
  }

  @SuppressWarnings("unused")
  public static class HttpRequestProcessorAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void methodEnter(
        @Advice.Argument(value = 0) HttpRequest httpRequest,
        @Advice.Argument(value = 1) HttpContext httpContext) {
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
        @Advice.Argument(value = 1) HttpContext httpContext) {
      Context context = httpContextManager().getCurrentContext(httpContext);
      if (context != null) {
        storage().storeHttpResponse(context, httpResponse);
      }
    }
  }
}
