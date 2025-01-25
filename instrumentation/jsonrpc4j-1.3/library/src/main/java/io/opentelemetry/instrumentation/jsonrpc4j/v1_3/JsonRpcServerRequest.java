/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.jsonrpc4j.v1_3;

import com.fasterxml.jackson.databind.JsonNode;
import java.lang.reflect.Method;
import java.util.List;

final class JsonRpcServerRequest {

  private final Method method;
  private final List<JsonNode> arguments;

  JsonRpcServerRequest(Method method, List<JsonNode> arguments) {
    this.method = method;
    this.arguments = arguments;
  }

  public Method getMethod() {
    return method;
  }

  public List<JsonNode> getArguments() {
    return arguments;
  }
}
