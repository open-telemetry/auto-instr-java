/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.spring.autoconfigure.internal.instrumentation.annotations;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.instrumentation.api.semconv.util.SpanNames;
import java.lang.reflect.Method;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

final class JoinPointRequest {

  private final JoinPoint joinPoint;
  private final Method method;
  private final String spanName;
  private final SpanKind spanKind;
  private final boolean inheritContext;

  private JoinPointRequest(
      JoinPoint joinPoint,
      Method method,
      String spanName,
      SpanKind spanKind,
      boolean inheritContext) {
    if (spanName.isEmpty()) {
      spanName = SpanNames.fromMethod(method);
    }

    this.joinPoint = joinPoint;
    this.method = method;
    this.spanName = spanName;
    this.spanKind = spanKind;
    this.inheritContext = inheritContext;
  }

  String spanName() {
    return spanName;
  }

  SpanKind spanKind() {
    return spanKind;
  }

  Method method() {
    return method;
  }

  Object[] args() {
    return joinPoint.getArgs();
  }

  boolean inheritContext() {
    return inheritContext;
  }

  interface Factory {

    JoinPointRequest create(JoinPoint joinPoint);
  }

  static final class InstrumentationAnnotationFactory implements Factory {

    @Override
    public JoinPointRequest create(JoinPoint joinPoint) {
      MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
      Method method = methodSignature.getMethod();

      // in rare cases, when interface method does not have annotations but the implementation does,
      // and the AspectJ factory is configured to proxy interfaces, this class will receive the
      // abstract interface method (without annotations) instead of the implementation method (with
      // annotations); these defaults prevent NPEs in this scenario
      WithSpan annotation = method.getDeclaredAnnotation(WithSpan.class);
      String spanName = annotation != null ? annotation.value() : "";
      SpanKind spanKind = annotation != null ? annotation.kind() : SpanKind.INTERNAL;
      boolean inheritContext = annotation == null || annotation.inheritContext();

      return new JoinPointRequest(joinPoint, method, spanName, spanKind, inheritContext);
    }
  }
}
