/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.apachehttpclient.v5_2;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.apachehttpclient.v5_2.internal.Experimental;
import io.opentelemetry.instrumentation.api.incubator.builder.internal.DefaultHttpClientInstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientAttributesExtractorBuilder;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.apache.hc.core5.http.HttpResponse;

/**
 * A builder for {@link ApacheHttpClient5Telemetry}.
 *
 * @deprecated Use {@link ApacheHttpClientTelemetryBuilder} instead.
 */
@Deprecated
public final class ApacheHttpClient5TelemetryBuilder {

  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.apache-httpclient-5.2";
  private final DefaultHttpClientInstrumenterBuilder<ApacheHttpClient5Request, HttpResponse>
      builder;
  private final OpenTelemetry openTelemetry;

  ApacheHttpClient5TelemetryBuilder(OpenTelemetry openTelemetry) {
    builder =
        DefaultHttpClientInstrumenterBuilder.create(
            INSTRUMENTATION_NAME, openTelemetry, ApacheHttpClient5HttpAttributesGetter.INSTANCE);
    this.openTelemetry = openTelemetry;
  }

  /**
   * Adds an additional {@link AttributesExtractor} to invoke to set attributes to instrumented
   * items. The {@link AttributesExtractor} will be executed after all default extractors.
   */
  @CanIgnoreReturnValue
  public ApacheHttpClient5TelemetryBuilder addAttributeExtractor(
      AttributesExtractor<? super ApacheHttpClient5Request, ? super HttpResponse>
          attributesExtractor) {
    builder.addAttributesExtractor(attributesExtractor);
    return this;
  }

  /**
   * Configures the HTTP request headers that will be captured as span attributes.
   *
   * @param requestHeaders A list of HTTP header names.
   */
  @CanIgnoreReturnValue
  public ApacheHttpClient5TelemetryBuilder setCapturedRequestHeaders(List<String> requestHeaders) {
    builder.setCapturedRequestHeaders(requestHeaders);
    return this;
  }

  /**
   * Configures the HTTP response headers that will be captured as span attributes.
   *
   * @param responseHeaders A list of HTTP header names.
   */
  @CanIgnoreReturnValue
  public ApacheHttpClient5TelemetryBuilder setCapturedResponseHeaders(
      List<String> responseHeaders) {
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
  public ApacheHttpClient5TelemetryBuilder setKnownMethods(Set<String> knownMethods) {
    builder.setKnownMethods(knownMethods);
    return this;
  }

  /**
   * Configures the instrumentation to emit experimental HTTP client metrics.
   *
   * @param emitExperimentalHttpClientMetrics {@code true} if the experimental HTTP client metrics
   *     are to be emitted.
   * @deprecated Use {@link
   *     Experimental#setEmitExperimentalTelemetry(ApacheHttpClientTelemetryBuilder, boolean)}
   *     instead.
   */
  @Deprecated
  @CanIgnoreReturnValue
  public ApacheHttpClient5TelemetryBuilder setEmitExperimentalHttpClientMetrics(
      boolean emitExperimentalHttpClientMetrics) {
    builder.setEmitExperimentalHttpClientMetrics(emitExperimentalHttpClientMetrics);
    return this;
  }

  /** Sets custom {@link SpanNameExtractor} via transform function. */
  @CanIgnoreReturnValue
  public ApacheHttpClient5TelemetryBuilder setSpanNameExtractor(
      Function<
              SpanNameExtractor<ApacheHttpClient5Request>,
              SpanNameExtractor<ApacheHttpClient5Request>>
          spanNameExtractorTransformer) {
    builder.setSpanNameExtractor(spanNameExtractorTransformer);
    return this;
  }

  /**
   * Returns a new {@link ApacheHttpClient5Telemetry} configured with this {@link
   * ApacheHttpClient5TelemetryBuilder}.
   */
  public ApacheHttpClient5Telemetry build() {
    return new ApacheHttpClient5Telemetry(builder.build(), openTelemetry.getPropagators());
  }
}
