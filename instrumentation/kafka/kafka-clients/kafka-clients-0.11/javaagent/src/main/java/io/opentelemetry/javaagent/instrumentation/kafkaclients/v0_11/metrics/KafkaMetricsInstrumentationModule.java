/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.kafkaclients.v0_11.metrics;

import static java.util.Arrays.asList;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.internal.ExperimentalInstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.internal.injection.ClassInjector;
import io.opentelemetry.javaagent.extension.instrumentation.internal.injection.InjectionMode;
import java.util.List;

@AutoService(InstrumentationModule.class)
public class KafkaMetricsInstrumentationModule extends InstrumentationModule
    implements ExperimentalInstrumentationModule {
  public KafkaMetricsInstrumentationModule() {
    super(
        "kafka-clients-metrics",
        "kafka-clients",
        "kafka-clients-metrics-0.11",
        "kafka-clients-0.11",
        "kafka");
  }

  @Override
  public void injectClasses(ClassInjector injector) {
    injector
        .proxyBuilder(
            "io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.OpenTelemetryMetricsReporter")
        .inject(InjectionMode.CLASS_ONLY);
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return asList(
        new KafkaMetricsProducerInstrumentation(), new KafkaMetricsConsumerInstrumentation());
  }
}
