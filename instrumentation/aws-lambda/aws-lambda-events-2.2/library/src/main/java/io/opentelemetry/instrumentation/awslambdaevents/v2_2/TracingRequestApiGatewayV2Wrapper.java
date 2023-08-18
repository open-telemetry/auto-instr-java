/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awslambdaevents.v2_2;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.opentelemetry.instrumentation.awslambdacore.v1_0.internal.WrappedLambda;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import java.util.function.BiFunction;

/**
 * Wrapper for {@link io.opentelemetry.instrumentation.awslambdacore.v1_0.TracingRequestHandler}.
 * Allows for wrapping a lambda proxied through API Gateway V2, enabling single span tracing and
 * HTTP context propagation.
 */
public class TracingRequestApiGatewayV2Wrapper
    extends TracingRequestWrapperBase<APIGatewayV2HTTPEvent, Object> {

  public TracingRequestApiGatewayV2Wrapper() {
    super(TracingRequestApiGatewayV2Wrapper::map);
  }

  // Visible for testing
  TracingRequestApiGatewayV2Wrapper(
      OpenTelemetrySdk openTelemetrySdk,
      WrappedLambda wrappedLambda,
      BiFunction<APIGatewayV2HTTPEvent, Class<?>, Object> mapper) {
    super(openTelemetrySdk, wrappedLambda, mapper);
  }

  // Visible for testing
  static <T> T map(APIGatewayV2HTTPEvent event, Class<T> clazz) {
    try {
      return OBJECT_MAPPER.readValue(event.getBody(), clazz);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException(
          "Could not map API Gateway V2 event body to requested parameter type: " + clazz, e);
    }
  }

  @Override
  protected APIGatewayV2HTTPResponse doHandleRequest(APIGatewayV2HTTPEvent input, Context context) {
    Object result = super.doHandleRequest(input, context);
    APIGatewayV2HTTPResponse event;
    // map to response event if needed
    if (result instanceof APIGatewayV2HTTPResponse) {
      event = (APIGatewayV2HTTPResponse) result;
    } else {
      try {
        event = new APIGatewayV2HTTPResponse();
        event.setBody(OBJECT_MAPPER.writeValueAsString(result));
      } catch (JsonProcessingException e) {
        throw new IllegalStateException("Could not serialize return value.", e);
      }
    }
    return event;
  }
}
