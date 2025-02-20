/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.kafka.internal;

import static io.opentelemetry.instrumentation.api.internal.SemconvStability.emitStableMessagingSemconv;
import static java.util.Collections.emptyList;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessageOperation;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesGetter;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingNetworkAttributesExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingSpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.ErrorCauseExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor;
import io.opentelemetry.instrumentation.api.internal.PropagatorBasedSpanLinksExtractor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.apache.kafka.clients.producer.RecordMetadata;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class KafkaInstrumenterFactory {

  private final OpenTelemetry openTelemetry;
  private final String instrumentationName;
  private ErrorCauseExtractor errorCauseExtractor = ErrorCauseExtractor.getDefault();
  private List<String> capturedHeaders = emptyList();
  private boolean captureExperimentalSpanAttributes = false;
  private boolean messagingReceiveInstrumentationEnabled = false;

  public KafkaInstrumenterFactory(OpenTelemetry openTelemetry, String instrumentationName) {
    this.openTelemetry = openTelemetry;
    this.instrumentationName = instrumentationName;
  }

  @CanIgnoreReturnValue
  public KafkaInstrumenterFactory setErrorCauseExtractor(ErrorCauseExtractor errorCauseExtractor) {
    this.errorCauseExtractor = errorCauseExtractor;
    return this;
  }

  @CanIgnoreReturnValue
  public KafkaInstrumenterFactory setCapturedHeaders(Collection<String> capturedHeaders) {
    this.capturedHeaders = new ArrayList<>(capturedHeaders);
    return this;
  }

  @CanIgnoreReturnValue
  public KafkaInstrumenterFactory setCaptureExperimentalSpanAttributes(
      boolean captureExperimentalSpanAttributes) {
    this.captureExperimentalSpanAttributes = captureExperimentalSpanAttributes;
    return this;
  }

  /**
   * @deprecated if you have a need for this configuration option please open an issue in the <a
   *     href="https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues">opentelemetry-java-instrumentation</a>
   *     repository.
   */
  @Deprecated
  @CanIgnoreReturnValue
  public KafkaInstrumenterFactory setPropagationEnabled(boolean propagationEnabled) {
    return this;
  }

  @CanIgnoreReturnValue
  public KafkaInstrumenterFactory setMessagingReceiveInstrumentationEnabled(
      boolean messagingReceiveInstrumentationEnabled) {
    this.messagingReceiveInstrumentationEnabled = messagingReceiveInstrumentationEnabled;
    return this;
  }

  public Instrumenter<KafkaProducerRequest, RecordMetadata> createProducerInstrumenter() {
    return createProducerInstrumenter(Collections.emptyList());
  }

  @SuppressWarnings("deprecation") // using deprecated semconv
  public Instrumenter<KafkaProducerRequest, RecordMetadata> createProducerInstrumenter(
      Iterable<AttributesExtractor<KafkaProducerRequest, RecordMetadata>> extractors) {

    KafkaProducerAttributesGetter getter = KafkaProducerAttributesGetter.INSTANCE;
    MessageOperation operation = MessageOperation.PUBLISH;

    return Instrumenter.<KafkaProducerRequest, RecordMetadata>builder(
            openTelemetry,
            instrumentationName,
            MessagingSpanNameExtractor.create(getter, operation, getter))
        .addAttributesExtractor(
            buildMessagingAttributesExtractor(getter, operation, capturedHeaders))
        .addAttributesExtractors(extractors)
        .addAttributesExtractor(new KafkaProducerAttributesExtractor())
        .addAttributesExtractor(MessagingNetworkAttributesExtractor.create(getter))
        .setErrorCauseExtractor(errorCauseExtractor)
        .buildInstrumenter(SpanKindExtractor.alwaysProducer());
  }

  public Instrumenter<KafkaReceiveRequest, Void> createConsumerReceiveInstrumenter() {
    return createConsumerReceiveInstrumenter(Collections.emptyList());
  }

  public Instrumenter<KafkaReceiveRequest, Void> createConsumerReceiveInstrumenter(
      Iterable<AttributesExtractor<KafkaReceiveRequest, Void>> extractors) {
    KafkaReceiveAttributesGetter getter = KafkaReceiveAttributesGetter.INSTANCE;
    MessageOperation operation = MessageOperation.RECEIVE;

    return Instrumenter.<KafkaReceiveRequest, Void>builder(
            openTelemetry,
            instrumentationName,
            MessagingSpanNameExtractor.create(getter, operation, getter))
        .addAttributesExtractor(
            buildMessagingAttributesExtractor(getter, operation, capturedHeaders))
        .addAttributesExtractor(KafkaReceiveAttributesExtractor.INSTANCE)
        .addAttributesExtractor(MessagingNetworkAttributesExtractor.create(getter))
        .addAttributesExtractors(extractors)
        .setErrorCauseExtractor(errorCauseExtractor)
        .setEnabled(messagingReceiveInstrumentationEnabled || emitStableMessagingSemconv())
        .buildInstrumenter(SpanKindExtractor.alwaysConsumer());
  }

  public Instrumenter<KafkaProcessRequest, Void> createConsumerProcessInstrumenter() {
    return createConsumerProcessInstrumenter(Collections.emptyList());
  }

  public Instrumenter<KafkaProcessRequest, Void> createConsumerProcessInstrumenter(
      Iterable<AttributesExtractor<KafkaProcessRequest, Void>> extractors) {
    KafkaConsumerAttributesGetter getter = KafkaConsumerAttributesGetter.INSTANCE;
    MessageOperation operation = MessageOperation.PROCESS;

    InstrumenterBuilder<KafkaProcessRequest, Void> builder =
        Instrumenter.<KafkaProcessRequest, Void>builder(
                openTelemetry,
                instrumentationName,
                MessagingSpanNameExtractor.create(getter, operation, getter))
            .addAttributesExtractor(
                buildMessagingAttributesExtractor(getter, operation, capturedHeaders))
            .addAttributesExtractor(new KafkaConsumerAttributesExtractor())
            .addAttributesExtractor(MessagingNetworkAttributesExtractor.create(getter))
            .addAttributesExtractors(extractors)
            .setErrorCauseExtractor(errorCauseExtractor);
    if (captureExperimentalSpanAttributes) {
      builder.addAttributesExtractor(new KafkaConsumerExperimentalAttributesExtractor());
    }

    if (messagingReceiveInstrumentationEnabled || emitStableMessagingSemconv()) {
      builder.addSpanLinksExtractor(
          new PropagatorBasedSpanLinksExtractor<>(
              openTelemetry.getPropagators().getTextMapPropagator(),
              KafkaConsumerRecordGetter.INSTANCE));
      return builder.buildInstrumenter(SpanKindExtractor.alwaysConsumer());
    } else {
      return builder.buildConsumerInstrumenter(KafkaConsumerRecordGetter.INSTANCE);
    }
  }

  public Instrumenter<KafkaReceiveRequest, Void> createBatchProcessInstrumenter() {
    KafkaReceiveAttributesGetter getter = KafkaReceiveAttributesGetter.INSTANCE;
    MessageOperation operation = MessageOperation.PROCESS;

    return Instrumenter.<KafkaReceiveRequest, Void>builder(
            openTelemetry,
            instrumentationName,
            MessagingSpanNameExtractor.create(getter, operation, getter))
        .addAttributesExtractor(
            buildMessagingAttributesExtractor(getter, operation, capturedHeaders))
        .addAttributesExtractor(KafkaReceiveAttributesExtractor.INSTANCE)
        .addAttributesExtractor(MessagingNetworkAttributesExtractor.create(getter))
        .addSpanLinksExtractor(
            new KafkaBatchProcessSpanLinksExtractor(
                openTelemetry.getPropagators().getTextMapPropagator()))
        .setErrorCauseExtractor(errorCauseExtractor)
        .buildInstrumenter(SpanKindExtractor.alwaysConsumer());
  }

  private static <REQUEST, RESPONSE>
      AttributesExtractor<REQUEST, RESPONSE> buildMessagingAttributesExtractor(
          MessagingAttributesGetter<REQUEST, RESPONSE> getter,
          MessageOperation operation,
          List<String> capturedHeaders) {
    return MessagingAttributesExtractor.builder(getter, operation)
        .setCapturedHeaders(capturedHeaders)
        .build();
  }
}
