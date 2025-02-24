/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.jsonrpc4j.v1_3;

import com.googlecode.jsonrpc4j.JsonRpcService;

@JsonRpcService("/calculator")
public interface CalculatorService {
  int add(int a, int b) throws Throwable;

  int subtract(int a, int b) throws Throwable;
}
