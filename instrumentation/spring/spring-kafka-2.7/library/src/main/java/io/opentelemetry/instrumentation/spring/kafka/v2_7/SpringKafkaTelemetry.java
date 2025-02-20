/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.spring.kafka.v2_7;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaProcessRequest;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaReceiveRequest;
import org.springframework.kafka.listener.AbstractMessageListenerContainer;
import org.springframework.kafka.listener.BatchInterceptor;
import org.springframework.kafka.listener.RecordInterceptor;

/** Entrypoint for instrumenting Spring Kafka listeners. */
public final class SpringKafkaTelemetry {

  /** Returns a new {@link SpringKafkaTelemetry} configured with the given {@link OpenTelemetry}. */
  public static SpringKafkaTelemetry create(OpenTelemetry openTelemetry) {
    return builder(openTelemetry).build();
  }

  /**
   * Returns a new {@link SpringKafkaTelemetryBuilder} configured with the given {@link
   * OpenTelemetry}.
   */
  public static SpringKafkaTelemetryBuilder builder(OpenTelemetry openTelemetry) {
    return new SpringKafkaTelemetryBuilder(openTelemetry);
  }

  private final Instrumenter<KafkaProcessRequest, Void> processInstrumenter;
  private final Instrumenter<KafkaReceiveRequest, Void> batchProcessInstrumenter;

  SpringKafkaTelemetry(
      Instrumenter<KafkaProcessRequest, Void> processInstrumenter,
      Instrumenter<KafkaReceiveRequest, Void> batchProcessInstrumenter) {
    this.processInstrumenter = processInstrumenter;
    this.batchProcessInstrumenter = batchProcessInstrumenter;
  }

  /**
   * Returns a new {@link RecordInterceptor} that decorates a message listener with a {@link
   * SpanKind#CONSUMER CONSUMER} span. Can be set on a {@link AbstractMessageListenerContainer}
   * using the {@link AbstractMessageListenerContainer#setRecordInterceptor(RecordInterceptor)}
   * method.
   */
  public <K, V> RecordInterceptor<K, V> createRecordInterceptor() {
    return createRecordInterceptor(null);
  }

  /**
   * Returns a new {@link RecordInterceptor} that decorates a message listener with a {@link
   * SpanKind#CONSUMER CONSUMER} span, and then delegates to a provided {@code
   * decoratedInterceptor}. Can be set on a {@link AbstractMessageListenerContainer} using the
   * {@link AbstractMessageListenerContainer#setRecordInterceptor(RecordInterceptor)} method.
   */
  public <K, V> RecordInterceptor<K, V> createRecordInterceptor(
      RecordInterceptor<K, V> decoratedInterceptor) {
    return new InstrumentedRecordInterceptor<>(processInstrumenter, decoratedInterceptor);
  }

  /**
   * Returns a new {@link BatchInterceptor} that decorates a message listener with a {@link
   * SpanKind#CONSUMER CONSUMER} span. Can be set on a {@link AbstractMessageListenerContainer}
   * using the {@link AbstractMessageListenerContainer#setBatchInterceptor(BatchInterceptor)}
   * method.
   */
  public <K, V> BatchInterceptor<K, V> createBatchInterceptor() {
    return createBatchInterceptor(null);
  }

  /**
   * Returns a new {@link BatchInterceptor} that decorates a message listener with a {@link
   * SpanKind#CONSUMER CONSUMER} span, and then delegates to a provided {@code
   * decoratedInterceptor}. Can be set on a {@link AbstractMessageListenerContainer} using the
   * {@link AbstractMessageListenerContainer#setBatchInterceptor(BatchInterceptor)} method.
   */
  public <K, V> BatchInterceptor<K, V> createBatchInterceptor(
      BatchInterceptor<K, V> decoratedInterceptor) {
    return new InstrumentedBatchInterceptor<>(batchProcessInstrumenter, decoratedInterceptor);
  }
}
