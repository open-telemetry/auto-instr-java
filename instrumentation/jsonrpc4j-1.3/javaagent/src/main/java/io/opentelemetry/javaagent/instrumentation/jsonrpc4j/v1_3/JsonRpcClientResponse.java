/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.jsonrpc4j.v1_3;

public final class JsonRpcClientResponse {

  private final Object result;

  public JsonRpcClientResponse(Object result) {
    this.result = result;
  }

  public Object getResult() {
    return result;
  }
}
