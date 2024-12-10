/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.ratpack.v1_7;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.incubator.builder.internal.DefaultHttpClientInstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanStatusExtractor;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientAttributesExtractorBuilder;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientTelemetryBuilder;
import io.opentelemetry.instrumentation.ratpack.v1_7.internal.Experimental;
import io.opentelemetry.instrumentation.ratpack.v1_7.internal.RatpackClientInstrumenterBuilderFactory;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import ratpack.http.client.HttpResponse;
import ratpack.http.client.RequestSpec;

/** A builder for {@link RatpackClientTelemetry}. */
public final class RatpackClientTelemetryBuilder
    implements HttpClientTelemetryBuilder<RequestSpec, HttpResponse> {

  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.ratpack-1.7";

  private final DefaultHttpClientInstrumenterBuilder<RequestSpec, HttpResponse> builder;

  RatpackClientTelemetryBuilder(OpenTelemetry openTelemetry) {
    builder = RatpackClientInstrumenterBuilderFactory.create(INSTRUMENTATION_NAME, openTelemetry);
  }

  @Override
  @CanIgnoreReturnValue
  public RatpackClientTelemetryBuilder addAttributesExtractor(
      AttributesExtractor<RequestSpec, HttpResponse> attributesExtractor) {
    builder.addAttributesExtractor(attributesExtractor);
    return this;
  }

  /**
   * Configures the HTTP client request headers that will be captured as span attributes.
   *
   * @param requestHeaders A list of HTTP header names.
   */
  @Override
  @CanIgnoreReturnValue
  public RatpackClientTelemetryBuilder setCapturedRequestHeaders(List<String> requestHeaders) {
    builder.setCapturedRequestHeaders(requestHeaders);
    return this;
  }

  /**
   * Configures the HTTP client response headers that will be captured as span attributes.
   *
   * @param responseHeaders A list of HTTP header names.
   */
  @Override
  @CanIgnoreReturnValue
  public RatpackClientTelemetryBuilder setCapturedResponseHeaders(List<String> responseHeaders) {
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
  public RatpackClientTelemetryBuilder setKnownMethods(Set<String> knownMethods) {
    builder.setKnownMethods(knownMethods);
    return this;
  }

  /** Sets custom client {@link SpanNameExtractor} via transform function. */
  @Override
  @CanIgnoreReturnValue
  public RatpackClientTelemetryBuilder setSpanNameExtractor(
      Function<SpanNameExtractor<RequestSpec>, SpanNameExtractor<RequestSpec>>
          clientSpanNameExtractor) {
    builder.setSpanNameExtractor(clientSpanNameExtractor);
    return this;
  }

  @Override
  public RatpackClientTelemetryBuilder setStatusExtractor(
      Function<
              SpanStatusExtractor<RequestSpec, HttpResponse>,
              SpanStatusExtractor<RequestSpec, HttpResponse>>
          statusExtractorTransformer) {
    builder.setStatusExtractor(statusExtractorTransformer);
    return this;
  }

  /**
   * Can be used via the unstable method {@link
   * Experimental#setEmitExperimentalTelemetry(RatpackClientTelemetryBuilder, boolean)}.
   */
  void setEmitExperimentalHttpClientMetrics(boolean emitExperimentalHttpClientMetrics) {
    builder.setEmitExperimentalHttpClientMetrics(emitExperimentalHttpClientMetrics);
  }

  /** Returns a new {@link RatpackClientTelemetry} with the configuration of this builder. */
  @Override
  public RatpackClientTelemetry build() {
    return new RatpackClientTelemetry(builder.build());
  }
}
