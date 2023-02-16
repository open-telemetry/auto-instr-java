/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.kafkastreams;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.kafka.internal.KafkaConsumerRequest;
import io.opentelemetry.instrumentation.kafka.internal.KafkaInstrumenterFactory;
import io.opentelemetry.javaagent.bootstrap.internal.ExperimentalConfig;
import io.opentelemetry.javaagent.bootstrap.internal.InstrumentationConfig;

public final class KafkaStreamsSingletons {

  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.kafka-streams-0.11";

  private static final Instrumenter<KafkaConsumerRequest, Void> INSTRUMENTER =
      new KafkaInstrumenterFactory(GlobalOpenTelemetry.get(), INSTRUMENTATION_NAME)
          .setCapturedHeaders(ExperimentalConfig.get().getMessagingHeaders())
          .setCaptureExperimentalSpanAttributes(
              InstrumentationConfig.get()
                  .getBoolean("otel.instrumentation.kafka.experimental-span-attributes", false))
          .setMessagingReceiveInstrumentationEnabled(
              ExperimentalConfig.get().messagingReceiveInstrumentationEnabled())
          .createConsumerProcessInstrumenter();

  public static Instrumenter<KafkaConsumerRequest, Void> instrumenter() {
    return INSTRUMENTER;
  }

  private KafkaStreamsSingletons() {}
}
