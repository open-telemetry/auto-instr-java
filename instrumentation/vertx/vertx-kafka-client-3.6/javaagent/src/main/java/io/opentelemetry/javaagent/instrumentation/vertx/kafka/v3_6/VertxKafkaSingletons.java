/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.vertx.kafka.v3_6;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.kafka.internal.KafkaInstrumenterFactory;
import io.opentelemetry.instrumentation.kafka.internal.KafkaProcessRequest;
import io.opentelemetry.instrumentation.kafka.internal.KafkaReceiveRequest;
import io.opentelemetry.javaagent.bootstrap.internal.AgentInstrumentationConfig;
import io.opentelemetry.javaagent.bootstrap.internal.ExperimentalConfig;
import org.apache.kafka.clients.producer.RecordMetadata;

public final class VertxKafkaSingletons {

  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.vertx-kafka-client-3.6";

  private static final Instrumenter<KafkaReceiveRequest, RecordMetadata> BATCH_PROCESS_INSTRUMENTER;
  private static final Instrumenter<KafkaProcessRequest, RecordMetadata> PROCESS_INSTRUMENTER;

  static {
    KafkaInstrumenterFactory factory =
        new KafkaInstrumenterFactory(GlobalOpenTelemetry.get(), INSTRUMENTATION_NAME)
            .setCapturedHeaders(ExperimentalConfig.get().getMessagingHeaders())
            .setCaptureExperimentalSpanAttributes(
                AgentInstrumentationConfig.get()
                    .getBoolean("otel.instrumentation.kafka.experimental-span-attributes", false))
            .setMessagingReceiveInstrumentationEnabled(
                ExperimentalConfig.get().messagingReceiveInstrumentationEnabled());
    BATCH_PROCESS_INSTRUMENTER = factory.createBatchProcessInstrumenter();
    PROCESS_INSTRUMENTER = factory.createConsumerProcessInstrumenter();
  }

  public static Instrumenter<KafkaReceiveRequest, RecordMetadata> batchProcessInstrumenter() {
    return BATCH_PROCESS_INSTRUMENTER;
  }

  public static Instrumenter<KafkaProcessRequest, RecordMetadata> processInstrumenter() {
    return PROCESS_INSTRUMENTER;
  }

  private VertxKafkaSingletons() {}
}
