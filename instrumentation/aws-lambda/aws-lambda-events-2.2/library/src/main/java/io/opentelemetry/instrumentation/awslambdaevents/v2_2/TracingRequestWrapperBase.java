/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awslambdaevents.v2_2;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.instrumentation.awslambdacore.v1_0.TracingRequestHandler;
import io.opentelemetry.instrumentation.awslambdacore.v1_0.internal.MapUtils;
import io.opentelemetry.instrumentation.awslambdacore.v1_0.internal.WrappedLambda;
import io.opentelemetry.instrumentation.awslambdacore.v1_0.internal.WrapperConfiguration;
import io.opentelemetry.instrumentation.awslambdaevents.v2_2.internal.AwsLambdaEventsInstrumenterFactory;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.autoconfigure.AutoConfiguredOpenTelemetrySdk;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Map;
import java.util.function.BiFunction;

/**
 * Base abstract wrapper for {@link TracingRequestHandler}. Provides: - delegation to a lambda via
 * env property OTEL_INSTRUMENTATION_AWS_LAMBDA_HANDLER in package.ClassName::methodName format
 */
abstract class TracingRequestWrapperBase<I, O> extends TracingRequestHandler<I, O> {

  protected static final ObjectMapper OBJECT_MAPPER =
      new ObjectMapper()
          .registerModule(new CustomJodaModule())
          .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  private final WrappedLambda wrappedLambda;
  private final Method targetMethod;
  private final BiFunction<I, Class<?>, Object> parameterMapper;

  protected TracingRequestWrapperBase(BiFunction<I, Class<?>, Object> parameterMapper) {
    this(
        AutoConfiguredOpenTelemetrySdk.initialize().getOpenTelemetrySdk(),
        WrappedLambda.fromConfiguration(),
        parameterMapper);
  }

  // Visible for testing
  TracingRequestWrapperBase(
      OpenTelemetrySdk openTelemetrySdk,
      WrappedLambda wrappedLambda,
      BiFunction<I, Class<?>, Object> parameterMapper) {
    super(
        openTelemetrySdk,
        WrapperConfiguration.flushTimeout(),
        AwsLambdaEventsInstrumenterFactory.createInstrumenter(openTelemetrySdk));
    this.wrappedLambda = wrappedLambda;
    this.targetMethod = wrappedLambda.getRequestTargetMethod();
    this.parameterMapper = parameterMapper;
  }

  @Override
  @SuppressWarnings("unchecked")
  protected O doHandleRequest(I input, Context context) {
    Object[] parameters = LambdaParameters.toArray(targetMethod, input, context, parameterMapper);
    O result;
    try {
      result = (O) targetMethod.invoke(wrappedLambda.getTargetObject(), parameters);
    } catch (IllegalAccessException e) {
      throw new IllegalStateException("Method is inaccessible", e);
    } catch (InvocationTargetException e) {
      throw (e.getCause() instanceof RuntimeException
          ? (RuntimeException) e.getCause()
          : new IllegalStateException(e.getTargetException()));
    }
    return result;
  }

  @Override
  protected final Map<String, String> extractHttpHeaders(I input) {
    if (input instanceof APIGatewayProxyRequestEvent) {
      return MapUtils.emptyIfNull(((APIGatewayProxyRequestEvent) input).getHeaders());
    } else if (input instanceof APIGatewayV2HTTPEvent) {
      return MapUtils.emptyIfNull(((APIGatewayV2HTTPEvent) input).getHeaders());
    }
    return Collections.emptyMap();
  }
}
