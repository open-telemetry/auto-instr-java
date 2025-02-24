/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.jsonrpc4j.v1_3;

import io.opentelemetry.instrumentation.api.incubator.semconv.rpc.RpcAttributesGetter;

// Check
// https://github.com/open-telemetry/semantic-conventions/blob/main/docs/rpc/rpc-metrics.md#attributes
// Check https://opentelemetry.io/docs/specs/semconv/rpc/json-rpc/
enum JsonRpcServerAttributesGetter implements RpcAttributesGetter<JsonRpcServerRequest> {
  INSTANCE;

  @Override
  public String getSystem(JsonRpcServerRequest request) {
    return "jsonrpc";
  }

  @Override
  public String getService(JsonRpcServerRequest request) {
    return request.getMethod().getDeclaringClass().getName();
  }

  @Override
  public String getMethod(JsonRpcServerRequest request) {
    return request.getMethod().getName();
  }
}
