/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.v5_0;

import static io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge.currentContext;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.implementsInterface;
import static io.opentelemetry.javaagent.instrumentation.apachehttpclient.v5_0.ApacheHttpClientSingletons.helper;
import static net.bytebuddy.matcher.ElementMatchers.isAbstract;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;

public final class ApacheHttpClientInstrumentation implements TypeInstrumentation {
  @Override
  public ElementMatcher<ClassLoader> classLoaderOptimization() {
    return hasClassesNamed("org.apache.hc.client5.http.classic.HttpClient");
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return implementsInterface(named("org.apache.hc.client5.http.classic.HttpClient"));
  }

  @Override
  public void transform(TypeTransformer transformer) {
    // There are 8 execute(...) methods. Depending on the version, they may or may not delegate
    // to each other. Thus, all methods need to be instrumented. Because of argument position and
    // type, some methods can share the same advice class. The call depth tracking ensures only 1
    // span is created
    transformer.applyAdviceToMethod(
        isMethod()
            .and(named("execute"))
            .and(not(isAbstract()))
            .and(takesArguments(1))
            .and(takesArgument(0, named("org.apache.hc.core5.http.ClassicHttpRequest"))),
        this.getClass().getName() + "$RequestAdvice");

    transformer.applyAdviceToMethod(
        isMethod()
            .and(named("execute"))
            .and(not(isAbstract()))
            .and(takesArguments(2))
            .and(takesArgument(0, named("org.apache.hc.core5.http.ClassicHttpRequest")))
            .and(takesArgument(1, named("org.apache.hc.core5.http.protocol.HttpContext"))),
        this.getClass().getName() + "$RequestAdvice");

    transformer.applyAdviceToMethod(
        isMethod()
            .and(named("execute"))
            .and(not(isAbstract()))
            .and(takesArguments(2))
            .and(takesArgument(0, named("org.apache.hc.core5.http.HttpHost")))
            .and(takesArgument(1, named("org.apache.hc.core5.http.ClassicHttpRequest"))),
        this.getClass().getName() + "$RequestWithHostAdvice");

    transformer.applyAdviceToMethod(
        isMethod()
            .and(named("execute"))
            .and(not(isAbstract()))
            .and(takesArguments(3))
            .and(takesArgument(0, named("org.apache.hc.core5.http.HttpHost")))
            .and(takesArgument(1, named("org.apache.hc.core5.http.ClassicHttpRequest")))
            .and(takesArgument(2, named("org.apache.hc.core5.http.protocol.HttpContext"))),
        this.getClass().getName() + "$RequestWithHostAdvice");

    transformer.applyAdviceToMethod(
        isMethod()
            .and(named("execute"))
            .and(not(isAbstract()))
            .and(takesArguments(2))
            .and(takesArgument(0, named("org.apache.hc.core5.http.ClassicHttpRequest")))
            .and(takesArgument(1, named("org.apache.hc.core5.http.io.HttpClientResponseHandler"))),
        this.getClass().getName() + "$RequestWithHandlerAdvice");

    transformer.applyAdviceToMethod(
        isMethod()
            .and(named("execute"))
            .and(not(isAbstract()))
            .and(takesArguments(3))
            .and(takesArgument(0, named("org.apache.hc.core5.http.ClassicHttpRequest")))
            .and(takesArgument(1, named("org.apache.hc.core5.http.protocol.HttpContext")))
            .and(takesArgument(2, named("org.apache.hc.core5.http.io.HttpClientResponseHandler"))),
        this.getClass().getName() + "$RequestWithContextAndHandlerAdvice");

    transformer.applyAdviceToMethod(
        isMethod()
            .and(named("execute"))
            .and(not(isAbstract()))
            .and(takesArguments(3))
            .and(takesArgument(0, named("org.apache.hc.core5.http.HttpHost")))
            .and(takesArgument(1, named("org.apache.hc.core5.http.ClassicHttpRequest")))
            .and(takesArgument(2, named("org.apache.hc.core5.http.io.HttpClientResponseHandler"))),
        this.getClass().getName() + "$RequestWithHostAndHandlerAdvice");

    transformer.applyAdviceToMethod(
        isMethod()
            .and(named("execute"))
            .and(not(isAbstract()))
            .and(takesArguments(4))
            .and(takesArgument(0, named("org.apache.hc.core5.http.HttpHost")))
            .and(takesArgument(1, named("org.apache.hc.core5.http.ClassicHttpRequest")))
            .and(takesArgument(2, named("org.apache.hc.core5.http.protocol.HttpContext")))
            .and(takesArgument(3, named("org.apache.hc.core5.http.io.HttpClientResponseHandler"))),
        this.getClass().getName() + "$RequestWithHostAndContextAndHandlerAdvice");
  }

  @SuppressWarnings("unused")
  public static class RequestAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void methodEnter(
        @Advice.Argument(0) ClassicHttpRequest request,
        @Advice.Local("otelRequest") ApacheHttpClientRequest otelRequest,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope) {
      Context parentContext = currentContext();
      otelRequest = new ApacheHttpClientRequest(request);
      context = helper().startInstrumentation(parentContext, otelRequest);

      if (context == null) {
        return;
      }

      scope = context.makeCurrent();
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void methodExit(
        @Advice.Return Object result,
        @Advice.Thrown Throwable throwable,
        @Advice.Local("otelRequest") ApacheHttpClientRequest otelRequest,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope) {
      if (scope == null) {
        return;
      }

      scope.close();
      helper().endInstrumentation(context, otelRequest, result, throwable);
    }
  }

  @SuppressWarnings("unused")
  public static class RequestWithHandlerAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void methodEnter(
        @Advice.Argument(0) ClassicHttpRequest request,
        @Advice.Argument(value = 1, readOnly = false) HttpClientResponseHandler<?> handler,
        @Advice.Local("otelRequest") ApacheHttpClientRequest otelRequest,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope) {
      Context parentContext = currentContext();
      otelRequest = new ApacheHttpClientRequest(request);
      context = helper().startInstrumentation(parentContext, otelRequest);

      if (context == null) {
        return;
      }

      scope = context.makeCurrent();

      // Wrap the handler so we capture the status code
      if (handler != null) {
        handler =
            new WrappingStatusSettingResponseHandler<>(
                context, parentContext, otelRequest, handler);
      }
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void methodExit(
        @Advice.Return Object result,
        @Advice.Thrown Throwable throwable,
        @Advice.Local("otelRequest") ApacheHttpClientRequest otelRequest,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope) {
      if (scope == null) {
        return;
      }

      scope.close();
      helper().endInstrumentation(context, otelRequest, result, throwable);
    }
  }

  @SuppressWarnings("unused")
  public static class RequestWithContextAndHandlerAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void methodEnter(
        @Advice.Argument(0) ClassicHttpRequest request,
        @Advice.Argument(value = 2, readOnly = false) HttpClientResponseHandler<?> handler,
        @Advice.Local("otelRequest") ApacheHttpClientRequest otelRequest,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope) {
      Context parentContext = currentContext();
      otelRequest = new ApacheHttpClientRequest(request);
      context = helper().startInstrumentation(parentContext, otelRequest);

      if (context == null) {
        return;
      }

      scope = context.makeCurrent();

      // Wrap the handler so we capture the status code
      if (handler != null) {
        handler =
            new WrappingStatusSettingResponseHandler<>(
                context, parentContext, otelRequest, handler);
      }
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void methodExit(
        @Advice.Return Object result,
        @Advice.Thrown Throwable throwable,
        @Advice.Local("otelRequest") ApacheHttpClientRequest otelRequest,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope) {
      if (scope == null) {
        return;
      }

      scope.close();
      helper().endInstrumentation(context, otelRequest, result, throwable);
    }
  }

  @SuppressWarnings("unused")
  public static class RequestWithHostAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void methodEnter(
        @Advice.Argument(0) HttpHost host,
        @Advice.Argument(1) ClassicHttpRequest request,
        @Advice.Local("otelRequest") ApacheHttpClientRequest otelRequest,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope) {
      Context parentContext = currentContext();
      otelRequest = new ApacheHttpClientRequest(host, request);
      context = helper().startInstrumentation(parentContext, otelRequest);

      if (context == null) {
        return;
      }

      scope = context.makeCurrent();
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void methodExit(
        @Advice.Return Object result,
        @Advice.Thrown Throwable throwable,
        @Advice.Local("otelRequest") ApacheHttpClientRequest otelRequest,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope) {
      if (scope == null) {
        return;
      }

      scope.close();
      helper().endInstrumentation(context, otelRequest, result, throwable);
    }
  }

  @SuppressWarnings("unused")
  public static class RequestWithHostAndHandlerAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void methodEnter(
        @Advice.Argument(0) HttpHost host,
        @Advice.Argument(1) ClassicHttpRequest request,
        @Advice.Argument(value = 2, readOnly = false) HttpClientResponseHandler<?> handler,
        @Advice.Local("otelRequest") ApacheHttpClientRequest otelRequest,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope) {
      Context parentContext = currentContext();
      otelRequest = new ApacheHttpClientRequest(host, request);
      context = helper().startInstrumentation(parentContext, otelRequest);

      if (context == null) {
        return;
      }

      scope = context.makeCurrent();

      // Wrap the handler so we capture the status code
      if (handler != null) {
        handler =
            new WrappingStatusSettingResponseHandler<>(
                context, parentContext, otelRequest, handler);
      }
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void methodExit(
        @Advice.Return Object result,
        @Advice.Thrown Throwable throwable,
        @Advice.Local("otelRequest") ApacheHttpClientRequest otelRequest,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope) {
      if (scope == null) {
        return;
      }

      scope.close();
      helper().endInstrumentation(context, otelRequest, result, throwable);
    }
  }

  @SuppressWarnings("unused")
  public static class RequestWithHostAndContextAndHandlerAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void methodEnter(
        @Advice.Argument(0) HttpHost host,
        @Advice.Argument(1) ClassicHttpRequest request,
        @Advice.Argument(value = 3, readOnly = false) HttpClientResponseHandler<?> handler,
        @Advice.Local("otelRequest") ApacheHttpClientRequest otelRequest,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope) {
      Context parentContext = currentContext();
      otelRequest = new ApacheHttpClientRequest(host, request);
      context = helper().startInstrumentation(parentContext, otelRequest);

      if (context == null) {
        return;
      }

      scope = context.makeCurrent();

      // Wrap the handler so we capture the status code
      if (handler != null) {
        handler =
            new WrappingStatusSettingResponseHandler<>(
                context, parentContext, otelRequest, handler);
      }
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void methodExit(
        @Advice.Return Object result,
        @Advice.Thrown Throwable throwable,
        @Advice.Local("otelRequest") ApacheHttpClientRequest otelRequest,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope) {
      if (scope == null) {
        return;
      }

      scope.close();
      helper().endInstrumentation(context, otelRequest, result, throwable);
    }
  }
}
