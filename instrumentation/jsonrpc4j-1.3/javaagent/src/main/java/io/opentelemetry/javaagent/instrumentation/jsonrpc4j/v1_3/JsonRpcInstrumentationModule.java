/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.jsonrpc4j.v1_3;

import static java.util.Arrays.asList;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import java.util.List;

@AutoService(InstrumentationModule.class)
public class JsonRpcInstrumentationModule extends InstrumentationModule {
  public JsonRpcInstrumentationModule() {
    super("jsonrpc4j", "jsonrpc4j-1.3");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return asList(
        new JsonRpcServerInstrumentation(),
        new JsonRpcClientInstrumentation(),
        new JsonRpcProxyInstrumentation());
  }
}
