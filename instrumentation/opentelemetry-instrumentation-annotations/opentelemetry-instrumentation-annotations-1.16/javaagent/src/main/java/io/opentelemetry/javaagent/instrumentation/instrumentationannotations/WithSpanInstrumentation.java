/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.instrumentationannotations;

import static io.opentelemetry.javaagent.instrumentation.instrumentationannotations.AnnotationSingletons.instrumenter;
import static io.opentelemetry.javaagent.instrumentation.instrumentationannotations.AnnotationSingletons.instrumenterWithAttributes;
import static io.opentelemetry.javaagent.instrumentation.instrumentationannotations.KotlinCoroutineUtil.isKotlinSuspendMethod;
import static net.bytebuddy.matcher.ElementMatchers.declaresMethod;
import static net.bytebuddy.matcher.ElementMatchers.hasParameters;
import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.whereAny;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.annotation.support.async.AsyncOperationEndSupport;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.lang.reflect.Method;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.annotation.AnnotationSource;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.assign.Assigner;
import net.bytebuddy.matcher.ElementMatcher;

class WithSpanInstrumentation implements TypeInstrumentation {

  private final ElementMatcher.Junction<AnnotationSource> annotatedMethodMatcher;
  private final ElementMatcher.Junction<MethodDescription> annotatedParametersMatcher;
  // this matcher matches all methods that should be excluded from transformation
  private final ElementMatcher.Junction<MethodDescription> excludedMethodsMatcher;

  WithSpanInstrumentation() {
    annotatedMethodMatcher =
        isAnnotatedWith(named("application.io.opentelemetry.instrumentation.annotations.WithSpan"));
    annotatedParametersMatcher =
        hasParameters(
            whereAny(
                isAnnotatedWith(
                    named(
                        "application.io.opentelemetry.instrumentation.annotations.SpanAttribute"))));
    // exclude all kotlin suspend methods, these are handle in kotlinx-coroutines instrumentation
    excludedMethodsMatcher =
        AnnotationExcludedMethods.configureExcludedMethods().or(isKotlinSuspendMethod());
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return declaresMethod(annotatedMethodMatcher);
  }

  @Override
  public void transform(TypeTransformer transformer) {
    ElementMatcher.Junction<MethodDescription> tracedMethods =
        annotatedMethodMatcher.and(not(excludedMethodsMatcher));

    ElementMatcher.Junction<MethodDescription> tracedMethodsWithParameters =
        tracedMethods.and(annotatedParametersMatcher);
    ElementMatcher.Junction<MethodDescription> tracedMethodsWithoutParameters =
        tracedMethods.and(not(annotatedParametersMatcher));

    transformer.applyAdviceToMethod(
        tracedMethodsWithoutParameters,
        WithSpanInstrumentation.class.getName() + "$WithSpanAdvice");

    // Only apply advice for tracing parameters as attributes if any of the parameters are annotated
    // with @SpanAttribute to avoid unnecessarily copying the arguments into an array.
    transformer.applyAdviceToMethod(
        tracedMethodsWithParameters,
        WithSpanInstrumentation.class.getName() + "$WithSpanAttributesAdvice");
  }

  @SuppressWarnings("unused")
  public static class WithSpanAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void onEnter(
        @Advice.Origin Method originMethod,
        @Advice.Local("otelMethod") Method method,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope) {
      // Every usage of @Advice.Origin Method is replaced with a call to Class.getMethod, copy it
      // to local variable so that there would be only one call to Class.getMethod.
      method = originMethod;

      Instrumenter<Method, Object> instrumenter = instrumenter();
      Context current = Java8BytecodeBridge.currentContext();

      if (instrumenter.shouldStart(current, method)) {
        context = instrumenter.start(current, method);
        scope = context.makeCurrent();
      }
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void stopSpan(
        @Advice.Local("otelMethod") Method method,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope,
        @Advice.Return(typing = Assigner.Typing.DYNAMIC, readOnly = false) Object returnValue,
        @Advice.Thrown Throwable throwable) {
      if (scope == null) {
        return;
      }
      scope.close();

      AsyncOperationEndSupport<Method, Object> operationEndSupport =
          AsyncOperationEndSupport.create(instrumenter(), Object.class, method.getReturnType());
      returnValue = operationEndSupport.asyncEnd(context, method, returnValue, throwable);
    }
  }

  @SuppressWarnings("unused")
  public static class WithSpanAttributesAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void onEnter(
        @Advice.Origin Method originMethod,
        @Advice.Local("otelMethod") Method method,
        @Advice.AllArguments(typing = Assigner.Typing.DYNAMIC) Object[] args,
        @Advice.Local("otelRequest") MethodRequest request,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope) {

      // Every usage of @Advice.Origin Method is replaced with a call to Class.getMethod, copy it
      // to local variable so that there would be only one call to Class.getMethod.
      method = originMethod;

      Instrumenter<MethodRequest, Object> instrumenter = instrumenterWithAttributes();
      Context current = Java8BytecodeBridge.currentContext();
      request = new MethodRequest(method, args);

      if (instrumenter.shouldStart(current, request)) {
        context = instrumenter.start(current, request);
        scope = context.makeCurrent();
      }
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void stopSpan(
        @Advice.Local("otelMethod") Method method,
        @Advice.Local("otelRequest") MethodRequest request,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope,
        @Advice.Return(typing = Assigner.Typing.DYNAMIC, readOnly = false) Object returnValue,
        @Advice.Thrown Throwable throwable) {
      if (scope == null) {
        return;
      }
      scope.close();
      AsyncOperationEndSupport<MethodRequest, Object> operationEndSupport =
          AsyncOperationEndSupport.create(
              instrumenterWithAttributes(), Object.class, method.getReturnType());
      returnValue = operationEndSupport.asyncEnd(context, request, returnValue, throwable);
    }
  }
}
