/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.grpc.v1_5;

import static java.util.Arrays.asList;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.tooling.InstrumentationModule;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@AutoService(InstrumentationModule.class)
public class GrpcInstrumentationModule extends InstrumentationModule {
  public GrpcInstrumentationModule() {
    super("grpc", "grpc-1.5");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return asList(
        new GrpcClientBuilderBuildInstrumentation(), new GrpcServerBuilderInstrumentation());
  }

  @Override
  protected Map<String, String> contextStore() {
    return Collections.singletonMap("io.grpc.ServerBuilder", Boolean.class.getName());
  }
}
