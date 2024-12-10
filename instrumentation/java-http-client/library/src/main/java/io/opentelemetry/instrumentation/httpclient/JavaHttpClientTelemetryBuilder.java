/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.httpclient;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.incubator.builder.internal.DefaultHttpClientInstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanStatusExtractor;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientAttributesExtractorBuilder;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientTelemetryBuilder;
import io.opentelemetry.instrumentation.httpclient.internal.Experimental;
import io.opentelemetry.instrumentation.httpclient.internal.HttpHeadersSetter;
import io.opentelemetry.instrumentation.httpclient.internal.JavaHttpClientInstrumenterBuilderFactory;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public final class JavaHttpClientTelemetryBuilder
    implements HttpClientTelemetryBuilder<HttpRequest, HttpResponse<?>> {

  private final DefaultHttpClientInstrumenterBuilder<HttpRequest, HttpResponse<?>> builder;
  private final OpenTelemetry openTelemetry;

  JavaHttpClientTelemetryBuilder(OpenTelemetry openTelemetry) {
    builder = JavaHttpClientInstrumenterBuilderFactory.create(openTelemetry);
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
  public JavaHttpClientTelemetryBuilder addAttributeExtractor(
      AttributesExtractor<? super HttpRequest, ? super HttpResponse<?>> attributesExtractor) {
    builder.addAttributesExtractor(attributesExtractor);
    return this;
  }

  /**
   * Adds an additional {@link AttributesExtractor} to invoke to set attributes to instrumented
   * items. The {@link AttributesExtractor} will be executed after all default extractors.
   */
  @Override
  @CanIgnoreReturnValue
  public JavaHttpClientTelemetryBuilder addAttributesExtractor(
      AttributesExtractor<HttpRequest, HttpResponse<?>> attributesExtractor) {
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
  public JavaHttpClientTelemetryBuilder setCapturedRequestHeaders(List<String> requestHeaders) {
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
  public JavaHttpClientTelemetryBuilder setCapturedResponseHeaders(List<String> responseHeaders) {
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
  public JavaHttpClientTelemetryBuilder setKnownMethods(Set<String> knownMethods) {
    builder.setKnownMethods(knownMethods);
    return this;
  }

  /**
   * Configures the instrumentation to emit experimental HTTP client metrics.
   *
   * @param emitExperimentalHttpClientMetrics {@code true} if the experimental HTTP client metrics
   *     are to be emitted.
   * @deprecated Use {@link
   *     Experimental#setEmitExperimentalTelemetry(JavaHttpClientTelemetryBuilder, boolean)}
   *     instead.
   */
  @Deprecated
  @CanIgnoreReturnValue
  public JavaHttpClientTelemetryBuilder setEmitExperimentalHttpClientMetrics(
      boolean emitExperimentalHttpClientMetrics) {
    builder.setEmitExperimentalHttpClientMetrics(emitExperimentalHttpClientMetrics);
    return this;
  }

  /** Sets custom {@link SpanNameExtractor} via transform function. */
  @Override
  @CanIgnoreReturnValue
  public JavaHttpClientTelemetryBuilder setSpanNameExtractor(
      Function<SpanNameExtractor<HttpRequest>, SpanNameExtractor<HttpRequest>>
          spanNameExtractorTransformer) {
    builder.setSpanNameExtractor(spanNameExtractorTransformer);
    return this;
  }

  @Override
  public JavaHttpClientTelemetryBuilder setStatusExtractor(
      Function<
              SpanStatusExtractor<HttpRequest, HttpResponse<?>>,
              SpanStatusExtractor<HttpRequest, HttpResponse<?>>>
          statusExtractorTransformer) {
    builder.setStatusExtractor(statusExtractorTransformer);
    return this;
  }

  @Override
  public JavaHttpClientTelemetry build() {
    return new JavaHttpClientTelemetry(
        builder.build(), new HttpHeadersSetter(openTelemetry.getPropagators()));
  }
}
