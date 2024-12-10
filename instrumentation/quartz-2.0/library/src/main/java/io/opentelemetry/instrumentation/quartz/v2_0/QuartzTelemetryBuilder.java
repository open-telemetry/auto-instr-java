/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.quartz.v2_0;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.instrumentation.api.incubator.semconv.code.CodeAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.quartz.JobExecutionContext;

/** A builder of {@link QuartzTelemetry}. */
public final class QuartzTelemetryBuilder {

  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.quartz-2.0";
  private final OpenTelemetry openTelemetry;
  private final List<AttributesExtractor<? super JobExecutionContext, ? super Void>>
      additionalExtractors = new ArrayList<>();
  private boolean captureExperimentalSpanAttributes;
  private Function<
          SpanNameExtractor<JobExecutionContext>,
          ? extends SpanNameExtractor<? super JobExecutionContext>>
      spanNameExtractorTransformer = Function.identity();

  QuartzTelemetryBuilder(OpenTelemetry openTelemetry) {
    this.openTelemetry = openTelemetry;
  }

  /**
   * Adds an additional {@link AttributesExtractor} to invoke to set attributes to instrumented
   * items. The {@link AttributesExtractor} will be executed after all default extractors.
   *
   * @deprecated Use {@link #addAttributesExtractor(AttributesExtractor)} instead.
   */
  @Deprecated
  @CanIgnoreReturnValue
  public QuartzTelemetryBuilder addAttributeExtractor(
      AttributesExtractor<? super JobExecutionContext, ? super Void> attributesExtractor) {
    additionalExtractors.add(attributesExtractor);
    return this;
  }

  /**
   * Adds an additional {@link AttributesExtractor} to invoke to set attributes to instrumented
   * items. The {@link AttributesExtractor} will be executed after all default extractors.
   */
  @CanIgnoreReturnValue
  public QuartzTelemetryBuilder addAttributesExtractor(
      AttributesExtractor<? super JobExecutionContext, ? super Void> attributesExtractor) {
    additionalExtractors.add(attributesExtractor);
    return this;
  }

  /**
   * Sets whether experimental attributes should be set to spans. These attributes may be changed or
   * removed in the future, so only enable this if you know you do not require attributes filled by
   * this instrumentation to be stable across versions
   */
  @CanIgnoreReturnValue
  public QuartzTelemetryBuilder setCaptureExperimentalSpanAttributes(
      boolean captureExperimentalSpanAttributes) {
    this.captureExperimentalSpanAttributes = captureExperimentalSpanAttributes;
    return this;
  }

  /** Sets custom {@link SpanNameExtractor} via transform function. */
  @CanIgnoreReturnValue
  public QuartzTelemetryBuilder setSpanNameExtractor(
      Function<
              SpanNameExtractor<JobExecutionContext>,
              ? extends SpanNameExtractor<? super JobExecutionContext>>
          spanNameExtractorTransformer) {
    this.spanNameExtractorTransformer = spanNameExtractorTransformer;
    return this;
  }

  /**
   * Returns a new {@link QuartzTelemetry} with the settings of this {@link QuartzTelemetryBuilder}.
   */
  public QuartzTelemetry build() {
    SpanNameExtractor<? super JobExecutionContext> spanNameExtractor =
        spanNameExtractorTransformer.apply(new QuartzSpanNameExtractor());

    InstrumenterBuilder<JobExecutionContext, Void> instrumenter =
        Instrumenter.builder(openTelemetry, INSTRUMENTATION_NAME, spanNameExtractor);

    if (captureExperimentalSpanAttributes) {
      instrumenter.addAttributesExtractor(
          AttributesExtractor.constant(AttributeKey.stringKey("job.system"), "quartz"));
    }
    instrumenter.setErrorCauseExtractor(new QuartzErrorCauseExtractor());
    instrumenter.addAttributesExtractor(
        CodeAttributesExtractor.create(new QuartzCodeAttributesGetter()));
    instrumenter.addAttributesExtractors(additionalExtractors);

    return new QuartzTelemetry(new TracingJobListener(instrumenter.buildInstrumenter()));
  }
}
