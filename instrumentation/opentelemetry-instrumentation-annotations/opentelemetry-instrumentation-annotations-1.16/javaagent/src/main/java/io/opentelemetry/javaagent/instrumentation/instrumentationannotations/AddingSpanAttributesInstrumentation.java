/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.instrumentationannotations;

import static io.opentelemetry.javaagent.instrumentation.instrumentationannotations.AnnotationSingletons.attributes;
import static net.bytebuddy.matcher.ElementMatchers.declaresMethod;
import static net.bytebuddy.matcher.ElementMatchers.hasParameters;
import static net.bytebuddy.matcher.ElementMatchers.isAnnotatedWith;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.whereAny;

import io.opentelemetry.api.trace.Span;
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

public class AddingSpanAttributesInstrumentation implements TypeInstrumentation {

  private final ElementMatcher.Junction<AnnotationSource> annotatedMethodMatcher;
  private final ElementMatcher.Junction<MethodDescription> annotatedParametersMatcher;
  // this matcher matches all methods that should be excluded from transformation
  private final ElementMatcher.Junction<MethodDescription> excludedMethodsMatcher;

  AddingSpanAttributesInstrumentation() {
    annotatedMethodMatcher =
        isAnnotatedWith(
                named(
                    "application.io.opentelemetry.instrumentation.annotations.AddingSpanAttributes"))
            // Avoid repeat extraction if method is already annotation with WithSpan
            .and(
                not(
                    isAnnotatedWith(
                        named(
                            "application.io.opentelemetry.instrumentation.annotations.WithSpan"))));
    annotatedParametersMatcher =
        hasParameters(
            whereAny(
                isAnnotatedWith(
                    named(
                        "application.io.opentelemetry.instrumentation.annotations.SpanAttribute"))));
    excludedMethodsMatcher = AnnotationExcludedMethods.configureExcludedMethods();
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return declaresMethod(annotatedMethodMatcher);
  }

  @Override
  public void transform(TypeTransformer transformer) {
    ElementMatcher.Junction<MethodDescription> tracedMethodsWithParameters =
        annotatedMethodMatcher.and(not(excludedMethodsMatcher)).and(annotatedParametersMatcher);

    transformer.applyAdviceToMethod(
        tracedMethodsWithParameters,
        AddingSpanAttributesInstrumentation.class.getName() + "$AddingSpanAttributesAdvice");
  }

  public static class AddingSpanAttributesAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void onEnter(
        @Advice.Origin Method method,
        @Advice.AllArguments(typing = Assigner.Typing.DYNAMIC) Object[] args) {
      Span otelSpan = Java8BytecodeBridge.currentSpan();
      if (otelSpan.isRecording() && otelSpan.getSpanContext().isValid()) {
        otelSpan.setAllAttributes(attributes().extract(method, args));
      }
    }
  }
}
