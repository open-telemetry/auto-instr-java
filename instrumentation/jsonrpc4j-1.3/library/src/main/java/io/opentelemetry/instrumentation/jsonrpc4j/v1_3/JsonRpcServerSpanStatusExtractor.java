/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.jsonrpc4j.v1_3;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.instrumentation.api.instrumenter.SpanStatusBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.SpanStatusExtractor;
import javax.annotation.Nullable;

enum JsonRpcServerSpanStatusExtractor
    implements SpanStatusExtractor<JsonRpcServerRequest, JsonRpcServerResponse> {
  INSTANCE;

  /** Extracts the status from the response and sets it to the {@code spanStatusBuilder}. */
  @Override
  public void extract(
      SpanStatusBuilder spanStatusBuilder,
      JsonRpcServerRequest jsonRpcServerRequest,
      @Nullable JsonRpcServerResponse jsonRpcServerResponse,
      @Nullable Throwable error) {
    // do not treat client invalid input as server error
    if ((error != null)
        && !(error instanceof JsonParseException)
        && !(error instanceof JsonMappingException)) {
      spanStatusBuilder.setStatus(StatusCode.ERROR);
    }
  }
}
