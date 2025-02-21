/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.aerospike;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static java.util.Arrays.asList;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import java.util.List;
import net.bytebuddy.matcher.ElementMatcher;

@AutoService(InstrumentationModule.class)
public class AerospikeClientInstrumentationModule extends InstrumentationModule {

  public AerospikeClientInstrumentationModule() {
    super("aerospike-client");
  }

  @Override
  public ElementMatcher.Junction<ClassLoader> classLoaderMatcher() {
    return hasClassesNamed("com.aerospike.client.metrics.LatencyType");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return asList(
        new AerospikeClientInstrumentation(),
        new SyncCommandInstrumentation(),
        new AsyncCommandInstrumentation());
  }
}
