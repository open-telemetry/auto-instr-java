/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.jsonrpc4j.v1_3;

import com.googlecode.jsonrpc4j.InvocationListener;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;

public final class JsonRpcServerTelemetry {
  public static JsonRpcServerTelemetry create(OpenTelemetry openTelemetry) {
    return builder(openTelemetry).build();
  }

  public static JsonRpcServerTelemetryBuilder builder(OpenTelemetry openTelemetry) {
    return new JsonRpcServerTelemetryBuilder(openTelemetry);
  }

  private final Instrumenter<JsonRpcServerRequest, JsonRpcServerResponse> serverInstrumenter;

  JsonRpcServerTelemetry(
      Instrumenter<JsonRpcServerRequest, JsonRpcServerResponse> serverInstrumenter) {
    this.serverInstrumenter = serverInstrumenter;
  }

  public InvocationListener newServerInvocationListener() {
    return new OpenTelemetryJsonRpcInvocationListener(serverInstrumenter);
  }
}
