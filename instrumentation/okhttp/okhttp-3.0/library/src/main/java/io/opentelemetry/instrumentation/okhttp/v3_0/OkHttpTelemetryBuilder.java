/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.okhttp.v3_0;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.incubator.builder.internal.DefaultHttpClientInstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientAttributesExtractorBuilder;
import io.opentelemetry.instrumentation.okhttp.v3_0.internal.Experimental;
import io.opentelemetry.instrumentation.okhttp.v3_0.internal.OkHttpClientInstrumenterBuilderFactory;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import okhttp3.Interceptor;
import okhttp3.Response;

/** A builder of {@link OkHttpTelemetry}. */
public final class OkHttpTelemetryBuilder {

  private final DefaultHttpClientInstrumenterBuilder<Interceptor.Chain, Response> builder;
  private final OpenTelemetry openTelemetry;

  static {
    Experimental.setSetEmitExperimentalTelemetry(
        (builder, emit) -> builder.builder.setEmitExperimentalHttpClientMetrics(emit));
  }

  OkHttpTelemetryBuilder(OpenTelemetry openTelemetry) {
    builder = OkHttpClientInstrumenterBuilderFactory.create(openTelemetry);
    this.openTelemetry = openTelemetry;
  }

  /**
   * Adds an additional {@link AttributesExtractor} to invoke to set attributes to instrumented
   * items.
   */
  @CanIgnoreReturnValue
  public OkHttpTelemetryBuilder addAttributeExtractor(
      AttributesExtractor<? super Interceptor.Chain, ? super Response> attributesExtractor) {
    builder.addAttributeExtractor(attributesExtractor);
    return this;
  }

  /**
   * Configures the HTTP request headers that will be captured as span attributes.
   *
   * @param requestHeaders A list of HTTP header names.
   */
  @CanIgnoreReturnValue
  public OkHttpTelemetryBuilder setCapturedRequestHeaders(List<String> requestHeaders) {
    builder.setCapturedRequestHeaders(requestHeaders);
    return this;
  }

  /**
   * Configures the HTTP response headers that will be captured as span attributes.
   *
   * @param responseHeaders A list of HTTP header names.
   */
  @CanIgnoreReturnValue
  public OkHttpTelemetryBuilder setCapturedResponseHeaders(List<String> responseHeaders) {
    builder.setCapturedResponseHeaders(responseHeaders);
    return this;
  }

  /**
   * Configures the instrumentation to recognize an alternative set of HTTP request methods.
   *
   * <p>By default, this instrumentation defines "known" methods as the ones listed in <a
   * href="https://www.rfc-editor.org/rfc/rfc9110.html#name-methods">RFC9110</a> and the PATCH
   * method defined in <a href="https://www.rfc-editor.org/rfc/rfc5789.html">RFC5789</a>.
   *
   * <p>Note: calling this method <b>overrides</b> the default known method sets completely; it does
   * not supplement it.
   *
   * @param knownMethods A set of recognized HTTP request methods.
   * @see HttpClientAttributesExtractorBuilder#setKnownMethods(Set)
   */
  @CanIgnoreReturnValue
  public OkHttpTelemetryBuilder setKnownMethods(Set<String> knownMethods) {
    builder.setKnownMethods(knownMethods);
    return this;
  }

  /**
   * Configures the instrumentation to emit experimental HTTP client metrics.
   *
   * @param emitExperimentalHttpClientMetrics {@code true} if the experimental HTTP client metrics
   *     are to be emitted.
   * @deprecated Use {@link Experimental#setEmitExperimentalTelemetry(OkHttpTelemetryBuilder,
   *     boolean)} instead.
   */
  @Deprecated
  @CanIgnoreReturnValue
  public OkHttpTelemetryBuilder setEmitExperimentalHttpClientMetrics(
      boolean emitExperimentalHttpClientMetrics) {
    builder.setEmitExperimentalHttpClientMetrics(emitExperimentalHttpClientMetrics);
    return this;
  }

  /** Sets custom {@link SpanNameExtractor} via transform function. */
  @CanIgnoreReturnValue
  public OkHttpTelemetryBuilder setSpanNameExtractor(
      Function<
              SpanNameExtractor<? super Interceptor.Chain>,
              ? extends SpanNameExtractor<? super Interceptor.Chain>>
          spanNameExtractorTransformer) {
    builder.setSpanNameExtractor(spanNameExtractorTransformer);
    return this;
  }

  /**
   * Returns a new {@link OkHttpTelemetry} with the settings of this {@link OkHttpTelemetryBuilder}.
   */
  public OkHttpTelemetry build() {
    return new OkHttpTelemetry(builder.build(), openTelemetry.getPropagators());
  }
}
