/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.jsonrpc4j.v1_3;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import javax.annotation.Nullable;

// Check https://opentelemetry.io/docs/specs/semconv/rpc/json-rpc/
final class JsonRpcClientAttributesExtractor
    implements AttributesExtractor<JsonRpcClientRequest, JsonRpcClientResponse> {

  // copied from RpcIncubatingAttributes
  private static final AttributeKey<String> RPC_JSONRPC_VERSION =
      AttributeKey.stringKey("rpc.jsonrpc.version");

  @Override
  public void onStart(
      AttributesBuilder attributes, Context parentContext, JsonRpcClientRequest jsonRpcRequest) {
    attributes.put(RPC_JSONRPC_VERSION, "2.0");
  }

  @Override
  public void onEnd(
      AttributesBuilder attributes,
      Context context,
      JsonRpcClientRequest jsonRpcRequest,
      @Nullable JsonRpcClientResponse jsonRpcResponse,
      @Nullable Throwable error) {}
}
