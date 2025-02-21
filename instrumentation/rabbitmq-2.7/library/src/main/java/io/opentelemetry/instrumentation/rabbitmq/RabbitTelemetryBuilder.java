/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.rabbitmq;

import static java.util.Collections.emptyList;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.api.OpenTelemetry;
import java.util.List;

/** A builder of {@link RabbitTelemetry}. */
public final class RabbitTelemetryBuilder {

  private final OpenTelemetry openTelemetry;

  private List<String> capturedHeaders = emptyList();
  private boolean captureExperimentalSpanAttributes;

  RabbitTelemetryBuilder(OpenTelemetry openTelemetry) {
    this.openTelemetry = openTelemetry;
  }

  /**
   * Sets whether experimental attributes should be set to spans. These attributes may be changed or
   * removed in the future, so only enable this if you know you do not require attributes filled by
   * this instrumentation to be stable across versions
   */
  @CanIgnoreReturnValue
  public RabbitTelemetryBuilder setCaptureExperimentalSpanAttributes(
      boolean captureExperimentalSpanAttributes) {
    this.captureExperimentalSpanAttributes = captureExperimentalSpanAttributes;
    return this;
  }

  /**
   * Configures the messaging headers that will be captured as span attributes.
   *
   * @param capturedHeaders A list of messaging header names.
   */
  @CanIgnoreReturnValue
  public RabbitTelemetryBuilder setCapturedHeaders(List<String> capturedHeaders) {
    this.capturedHeaders = capturedHeaders;
    return this;
  }

  /**
   * Returns a new {@link RabbitTelemetry} with the settings of this {@link RabbitTelemetryBuilder}.
   */
  public RabbitTelemetry build() {
    return new RabbitTelemetry(openTelemetry, capturedHeaders, captureExperimentalSpanAttributes);
  }
}
