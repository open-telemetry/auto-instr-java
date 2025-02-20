/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.spring.kafka.v2_7;

import static java.util.Collections.emptyList;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaInstrumenterFactory;
import io.opentelemetry.instrumentation.spring.kafka.v2_7.internal.SpringKafkaErrorCauseExtractor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/** A builder of {@link SpringKafkaTelemetry}. */
public final class SpringKafkaTelemetryBuilder {

  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.spring-kafka-2.7";

  private final OpenTelemetry openTelemetry;
  private List<String> capturedHeaders = emptyList();
  private boolean captureExperimentalSpanAttributes = false;
  private boolean messagingReceiveInstrumentationEnabled = false;

  SpringKafkaTelemetryBuilder(OpenTelemetry openTelemetry) {
    this.openTelemetry = openTelemetry;
  }

  @CanIgnoreReturnValue
  public SpringKafkaTelemetryBuilder setCapturedHeaders(Collection<String> capturedHeaders) {
    this.capturedHeaders = new ArrayList<>(capturedHeaders);
    return this;
  }

  @CanIgnoreReturnValue
  public SpringKafkaTelemetryBuilder setCaptureExperimentalSpanAttributes(
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
  public SpringKafkaTelemetryBuilder setPropagationEnabled(boolean propagationEnabled) {
    return this;
  }

  /**
   * Set whether to capture the consumer message receive telemetry in messaging instrumentation.
   *
   * <p>Note that this will cause the consumer side to start a new trace, with only a span link
   * connecting it to the producer trace.
   */
  @CanIgnoreReturnValue
  public SpringKafkaTelemetryBuilder setMessagingReceiveInstrumentationEnabled(
      boolean messagingReceiveInstrumentationEnabled) {
    this.messagingReceiveInstrumentationEnabled = messagingReceiveInstrumentationEnabled;
    return this;
  }

  /**
   * Returns a new {@link SpringKafkaTelemetry} with the settings of this {@link
   * SpringKafkaTelemetryBuilder}.
   */
  public SpringKafkaTelemetry build() {
    KafkaInstrumenterFactory factory =
        new KafkaInstrumenterFactory(openTelemetry, INSTRUMENTATION_NAME)
            .setCapturedHeaders(capturedHeaders)
            .setCaptureExperimentalSpanAttributes(captureExperimentalSpanAttributes)
            .setMessagingReceiveInstrumentationEnabled(messagingReceiveInstrumentationEnabled)
            .setErrorCauseExtractor(SpringKafkaErrorCauseExtractor.INSTANCE);

    return new SpringKafkaTelemetry(
        factory.createConsumerProcessInstrumenter(), factory.createBatchProcessInstrumenter());
  }
}
