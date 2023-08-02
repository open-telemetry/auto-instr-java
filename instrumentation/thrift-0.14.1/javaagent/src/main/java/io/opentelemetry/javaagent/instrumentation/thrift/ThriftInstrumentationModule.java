/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.thrift;

import static java.util.Arrays.asList;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import java.util.List;

@AutoService(InstrumentationModule.class)
public final class ThriftInstrumentationModule extends InstrumentationModule {
  public ThriftInstrumentationModule() {
    super("thrift", "thrift-0.14.1");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {

    return asList(
        new ThriftClientInstrumentation(),
        new ThriftServerInstrumentation(),
        new ThriftBaseProcessorInstrumentation(),
        new ThriftAsyncClientInstrumentation(),
        new ThriftBaseAsyncProcessorInstrumentation(),
        new ThriftAsyncMethodCallInstrumentation());
  }
}
