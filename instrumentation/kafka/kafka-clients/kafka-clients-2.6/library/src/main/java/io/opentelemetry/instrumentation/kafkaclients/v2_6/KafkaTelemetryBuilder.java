/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.kafkaclients.v2_6;

import static java.util.Collections.emptyList;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaInstrumenterFactory;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaProcessRequest;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaProducerRequest;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaReceiveRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import org.apache.kafka.clients.producer.RecordMetadata;

public final class KafkaTelemetryBuilder {
  static final String INSTRUMENTATION_NAME = "io.opentelemetry.kafka-clients-2.6";

  private final OpenTelemetry openTelemetry;
  private final List<AttributesExtractor<KafkaProducerRequest, RecordMetadata>>
      producerAttributesExtractors = new ArrayList<>();
  private final List<AttributesExtractor<KafkaProcessRequest, Void>>
      consumerProcessAttributesExtractors = new ArrayList<>();
  private final List<AttributesExtractor<KafkaReceiveRequest, Void>>
      consumerReceiveAttributesExtractors = new ArrayList<>();
  private List<String> capturedHeaders = emptyList();
  private boolean captureExperimentalSpanAttributes = false;
  private boolean propagationEnabled = true;
  private boolean messagingReceiveInstrumentationEnabled = false;

  KafkaTelemetryBuilder(OpenTelemetry openTelemetry) {
    this.openTelemetry = Objects.requireNonNull(openTelemetry);
  }

  @CanIgnoreReturnValue
  public KafkaTelemetryBuilder addProducerAttributesExtractors(
      AttributesExtractor<KafkaProducerRequest, RecordMetadata> extractor) {
    producerAttributesExtractors.add(extractor);
    return this;
  }

  /** Use {@link #addConsumerProcessAttributesExtractors(AttributesExtractor)} instead. */
  @Deprecated
  @CanIgnoreReturnValue
  public KafkaTelemetryBuilder addConsumerAttributesExtractors(
      AttributesExtractor<KafkaProcessRequest, Void> extractor) {
    return addConsumerProcessAttributesExtractors(extractor);
  }

  @CanIgnoreReturnValue
  public KafkaTelemetryBuilder addConsumerProcessAttributesExtractors(
      AttributesExtractor<KafkaProcessRequest, Void> extractor) {
    consumerProcessAttributesExtractors.add(extractor);
    return this;
  }

  @CanIgnoreReturnValue
  public KafkaTelemetryBuilder addConsumerReceiveAttributesExtractors(
      AttributesExtractor<KafkaReceiveRequest, Void> extractor) {
    consumerReceiveAttributesExtractors.add(extractor);
    return this;
  }

  /**
   * Configures the messaging headers that will be captured as span attributes.
   *
   * @param capturedHeaders A list of messaging header names.
   */
  @CanIgnoreReturnValue
  public KafkaTelemetryBuilder setCapturedHeaders(Collection<String> capturedHeaders) {
    this.capturedHeaders = new ArrayList<>(capturedHeaders);
    return this;
  }

  /**
   * Sets whether experimental attributes should be set to spans. These attributes may be changed or
   * removed in the future, so only enable this if you know you do not require attributes filled by
   * this instrumentation to be stable across versions.
   */
  @CanIgnoreReturnValue
  public KafkaTelemetryBuilder setCaptureExperimentalSpanAttributes(
      boolean captureExperimentalSpanAttributes) {
    this.captureExperimentalSpanAttributes = captureExperimentalSpanAttributes;
    return this;
  }

  /**
   * Set whether to propagate trace context in producers. Enabled by default.
   *
   * <p>You will need to disable this if there are kafka consumers using kafka-clients version prior
   * to 0.11, since those old versions do not support headers, and attaching trace context
   * propagation headers upstream causes those consumers to fail when reading the messages.
   */
  @CanIgnoreReturnValue
  public KafkaTelemetryBuilder setPropagationEnabled(boolean propagationEnabled) {
    this.propagationEnabled = propagationEnabled;
    return this;
  }

  /**
   * Set whether to capture the consumer message receive telemetry in messaging instrumentation.
   *
   * <p>Note that this will cause the consumer side to start a new trace, with only a span link
   * connecting it to the producer trace.
   */
  @CanIgnoreReturnValue
  public KafkaTelemetryBuilder setMessagingReceiveInstrumentationEnabled(
      boolean messagingReceiveInstrumentationEnabled) {
    this.messagingReceiveInstrumentationEnabled = messagingReceiveInstrumentationEnabled;
    return this;
  }

  public KafkaTelemetry build() {
    KafkaInstrumenterFactory instrumenterFactory =
        new KafkaInstrumenterFactory(openTelemetry, INSTRUMENTATION_NAME)
            .setCapturedHeaders(capturedHeaders)
            .setCaptureExperimentalSpanAttributes(captureExperimentalSpanAttributes)
            .setMessagingReceiveInstrumentationEnabled(messagingReceiveInstrumentationEnabled);

    return new KafkaTelemetry(
        openTelemetry,
        instrumenterFactory.createProducerInstrumenter(producerAttributesExtractors),
        instrumenterFactory.createConsumerReceiveInstrumenter(consumerReceiveAttributesExtractors),
        instrumenterFactory.createConsumerProcessInstrumenter(consumerProcessAttributesExtractors),
        propagationEnabled);
  }
}
