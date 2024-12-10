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
import io.opentelemetry.instrumentation.api.instrumenter.SpanStatusExtractor;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientAttributesExtractorBuilder;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientTelemetryBuilder;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.apache.hc.core5.http.HttpResponse;

/** A builder for {@link ApacheHttpClientTelemetry}. */
public final class ApacheHttpClientTelemetryBuilder
    implements HttpClientTelemetryBuilder<ApacheHttpClientRequest, HttpResponse> {

  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.apache-httpclient-5.2";
  private final DefaultHttpClientInstrumenterBuilder<ApacheHttpClientRequest, HttpResponse> builder;
  private final OpenTelemetry openTelemetry;

  ApacheHttpClientTelemetryBuilder(OpenTelemetry openTelemetry) {
    builder =
        DefaultHttpClientInstrumenterBuilder.create(
            INSTRUMENTATION_NAME, openTelemetry, ApacheHttpClientHttpAttributesGetter.INSTANCE);
    this.openTelemetry = openTelemetry;
  }

  /**
   * Adds an additional {@link AttributesExtractor} to invoke to set attributes to instrumented
   * items. The {@link AttributesExtractor} will be executed after all default extractors.
   */
  @Override
  @CanIgnoreReturnValue
  public ApacheHttpClientTelemetryBuilder addAttributesExtractor(
      AttributesExtractor<ApacheHttpClientRequest, HttpResponse> attributesExtractor) {
    builder.addAttributesExtractor(attributesExtractor);
    return this;
  }

  /**
   * Configures the HTTP request headers that will be captured as span attributes.
   *
   * @param requestHeaders A list of HTTP header names.
   */
  @Override
  @CanIgnoreReturnValue
  public ApacheHttpClientTelemetryBuilder setCapturedRequestHeaders(List<String> requestHeaders) {
    builder.setCapturedRequestHeaders(requestHeaders);
    return this;
  }

  /**
   * Configures the HTTP response headers that will be captured as span attributes.
   *
   * @param responseHeaders A list of HTTP header names.
   */
  @Override
  @CanIgnoreReturnValue
  public ApacheHttpClientTelemetryBuilder setCapturedResponseHeaders(List<String> responseHeaders) {
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
  @Override
  @CanIgnoreReturnValue
  public ApacheHttpClientTelemetryBuilder setKnownMethods(Set<String> knownMethods) {
    builder.setKnownMethods(knownMethods);
    return this;
  }

  /** Sets custom {@link SpanNameExtractor} via transform function. */
  @Override
  @CanIgnoreReturnValue
  public ApacheHttpClientTelemetryBuilder setSpanNameExtractor(
      Function<
              SpanNameExtractor<ApacheHttpClientRequest>,
              SpanNameExtractor<ApacheHttpClientRequest>>
          spanNameExtractorTransformer) {
    builder.setSpanNameExtractor(spanNameExtractorTransformer);
    return this;
  }

  @Override
  public ApacheHttpClientTelemetryBuilder setStatusExtractor(
      Function<
              SpanStatusExtractor<ApacheHttpClientRequest, HttpResponse>,
              SpanStatusExtractor<ApacheHttpClientRequest, HttpResponse>>
          spanNameExtractorTransformer) {
    builder.setStatusExtractor(spanNameExtractorTransformer);
    return this;
  }

  /**
   * Can be used via the unstable method {@link
   * Experimental#setEmitExperimentalTelemetry(ApacheHttpClientTelemetryBuilder, boolean)}.
   */
  void setEmitExperimentalHttpClientMetrics(boolean emitExperimentalHttpClientMetrics) {
    builder.setEmitExperimentalHttpClientMetrics(emitExperimentalHttpClientMetrics);
  }

  /**
   * Returns a new {@link ApacheHttpClientTelemetry} configured with this {@link
   * ApacheHttpClientTelemetryBuilder}.
   */
  @Override
  public ApacheHttpClientTelemetry build() {
    return new ApacheHttpClientTelemetry(builder.build(), openTelemetry.getPropagators());
  }
}
