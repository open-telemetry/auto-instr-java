/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.spring.autoconfigure.internal.instrumentation.annotations;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import io.opentelemetry.instrumentation.api.annotation.support.MethodSpanAttributesExtractor;
import io.opentelemetry.instrumentation.api.annotation.support.ParameterAttributeNamesExtractor;
import io.opentelemetry.instrumentation.api.annotation.support.async.AsyncOperationEndSupport;
import io.opentelemetry.instrumentation.api.incubator.semconv.code.CodeAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.core.ParameterNameDiscoverer;

/**
 * Uses Spring-AOP to wrap methods marked by {@link WithSpan} in a {@link Span}.
 *
 * <p>Ensure methods annotated with {@link WithSpan} are implemented on beans managed by the Spring
 * container.
 *
 * <p>Note: This Aspect uses spring-aop to proxy beans. Therefore, the {@link WithSpan} annotation
 * can not be applied to constructors.
 */
abstract class WithSpanAspect {
  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.spring-boot-autoconfigure";

  private final Instrumenter<JoinPointRequest, Object> instrumenter;
  private final JoinPointRequest.Factory requestFactory;

  WithSpanAspect(
      OpenTelemetry openTelemetry,
      ParameterNameDiscoverer parameterNameDiscoverer,
      JoinPointRequest.Factory requestFactory,
      WithSpanAspectParameterAttributeNamesExtractor.SpanAttributeNameSupplier
          spanAttributeNameSupplier) {

    ParameterAttributeNamesExtractor parameterAttributeNamesExtractor =
        new WithSpanAspectParameterAttributeNamesExtractor(
            parameterNameDiscoverer, spanAttributeNameSupplier);

    instrumenter =
        Instrumenter.builder(openTelemetry, INSTRUMENTATION_NAME, JoinPointRequest::spanName)
            .addAttributesExtractor(
                CodeAttributesExtractor.create(JointPointCodeAttributesExtractor.INSTANCE))
            .addAttributesExtractor(
                MethodSpanAttributesExtractor.create(
                    JoinPointRequest::method,
                    parameterAttributeNamesExtractor,
                    JoinPointRequest::args))
            .addContextCustomizer(WithSpanAspect::parentContext)
            .buildInstrumenter(JoinPointRequest::spanKind);
    this.requestFactory = requestFactory;
  }

  private static Context parentContext(
      Context parentContext, JoinPointRequest request, Attributes unused) {
    return request.inheritContext() ? parentContext : Context.root();
  }

  public Object traceMethod(ProceedingJoinPoint pjp) throws Throwable {
    JoinPointRequest request = requestFactory.create(pjp);
    Context parentContext = Context.current();
    if (!instrumenter.shouldStart(parentContext, request)) {
      return pjp.proceed();
    }

    Context context = instrumenter.start(parentContext, request);
    AsyncOperationEndSupport<JoinPointRequest, Object> asyncOperationEndSupport =
        AsyncOperationEndSupport.create(
            instrumenter, Object.class, request.method().getReturnType());

    Object response;
    try (Scope ignored = context.makeCurrent()) {
      response = pjp.proceed();
    } catch (Throwable t) {
      asyncOperationEndSupport.asyncEnd(context, request, null, t);
      throw t;
    }
    return asyncOperationEndSupport.asyncEnd(context, request, response, null);
  }
}
