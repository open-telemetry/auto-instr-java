/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.reactor.kafka.v1_0;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaInstrumenterFactory;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaProcessRequest;
import io.opentelemetry.javaagent.bootstrap.internal.AgentInstrumentationConfig;
import io.opentelemetry.javaagent.bootstrap.internal.ExperimentalConfig;

final class ReactorKafkaSingletons {

  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.reactor-kafka-1.0";

  private static final Instrumenter<KafkaProcessRequest, Void> PROCESS_INSTRUMENTER =
      new KafkaInstrumenterFactory(GlobalOpenTelemetry.get(), INSTRUMENTATION_NAME)
          .setCapturedHeaders(ExperimentalConfig.get().getMessagingHeaders())
          .setCaptureExperimentalSpanAttributes(
              AgentInstrumentationConfig.get()
                  .getBoolean("otel.instrumentation.kafka.experimental-span-attributes", false))
          .setMessagingReceiveInstrumentationEnabled(
              ExperimentalConfig.get().messagingReceiveInstrumentationEnabled())
          .createConsumerProcessInstrumenter();

  public static Instrumenter<KafkaProcessRequest, Void> processInstrumenter() {
    return PROCESS_INSTRUMENTER;
  }

  private ReactorKafkaSingletons() {}
}
