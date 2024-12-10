/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.restlet.v2_0;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.incubator.builder.internal.DefaultHttpServerInstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanStatusExtractor;
import io.opentelemetry.instrumentation.api.semconv.http.HttpServerAttributesExtractorBuilder;
import io.opentelemetry.instrumentation.api.semconv.http.HttpServerTelemetryBuilder;
import io.opentelemetry.instrumentation.restlet.v2_0.internal.Experimental;
import io.opentelemetry.instrumentation.restlet.v2_0.internal.RestletTelemetryBuilderFactory;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.restlet.Request;
import org.restlet.Response;

/** A builder of {@link RestletTelemetry}. */
public final class RestletTelemetryBuilder
    implements HttpServerTelemetryBuilder<Request, Response> {

  private final DefaultHttpServerInstrumenterBuilder<Request, Response> builder;

  RestletTelemetryBuilder(OpenTelemetry openTelemetry) {
    builder = RestletTelemetryBuilderFactory.create(openTelemetry);
  }

  /**
   * Adds an additional {@link AttributesExtractor} to invoke to set attributes to instrumented
   * items.
   */
  @Override
  @CanIgnoreReturnValue
  public RestletTelemetryBuilder addAttributesExtractor(
      AttributesExtractor<Request, Response> attributesExtractor) {
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
  public RestletTelemetryBuilder setCapturedRequestHeaders(List<String> requestHeaders) {
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
  public RestletTelemetryBuilder setCapturedResponseHeaders(List<String> responseHeaders) {
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
   * @see HttpServerAttributesExtractorBuilder#setKnownMethods(Set)
   */
  @Override
  @CanIgnoreReturnValue
  public RestletTelemetryBuilder setKnownMethods(Set<String> knownMethods) {
    builder.setKnownMethods(knownMethods);
    return this;
  }

  /**
   * Configures the instrumentation to emit experimental HTTP server metrics.
   *
   * @param emitExperimentalHttpServerMetrics {@code true} if the experimental HTTP server metrics
   *     are to be emitted.
   * @deprecated Use {@link Experimental#setEmitExperimentalTelemetry(RestletTelemetryBuilder,
   *     boolean)} instead.
   */
  @Deprecated
  @CanIgnoreReturnValue
  public RestletTelemetryBuilder setEmitExperimentalHttpServerMetrics(
      boolean emitExperimentalHttpServerMetrics) {
    builder.setEmitExperimentalHttpServerMetrics(emitExperimentalHttpServerMetrics);
    return this;
  }

  /** Sets custom {@link SpanNameExtractor} via transform function. */
  @Override
  @CanIgnoreReturnValue
  public RestletTelemetryBuilder setSpanNameExtractor(
      Function<SpanNameExtractor<Request>, SpanNameExtractor<Request>>
          spanNameExtractorTransformer) {
    builder.setSpanNameExtractor(spanNameExtractorTransformer);
    return this;
  }

  @Override
  public RestletTelemetryBuilder setStatusExtractor(
      Function<SpanStatusExtractor<Request, Response>, SpanStatusExtractor<Request, Response>>
          statusExtractorTransformer) {
    builder.setStatusExtractor(statusExtractorTransformer);
    return this;
  }

  /**
   * Returns a new {@link RestletTelemetry} with the settings of this {@link
   * RestletTelemetryBuilder}.
   */
  @Override
  public RestletTelemetry build() {
    return new RestletTelemetry(builder.build());
  }
}
