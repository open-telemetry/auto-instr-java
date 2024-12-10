/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.spring.webmvc.v6_0;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.incubator.builder.internal.DefaultHttpServerInstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanStatusExtractor;
import io.opentelemetry.instrumentation.api.semconv.http.HttpServerAttributesExtractorBuilder;
import io.opentelemetry.instrumentation.api.semconv.http.HttpServerTelemetryBuilder;
import io.opentelemetry.instrumentation.spring.webmvc.v6_0.internal.Experimental;
import io.opentelemetry.instrumentation.spring.webmvc.v6_0.internal.SpringMvcBuilderUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/** A builder of {@link SpringWebMvcTelemetry}. */
public final class SpringWebMvcTelemetryBuilder
    implements HttpServerTelemetryBuilder<HttpServletRequest, HttpServletResponse> {

  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.spring-webmvc-6.0";
  private final DefaultHttpServerInstrumenterBuilder<HttpServletRequest, HttpServletResponse>
      builder;

  static {
    SpringMvcBuilderUtil.setBuilderExtractor(builder -> builder.builder);
  }

  SpringWebMvcTelemetryBuilder(OpenTelemetry openTelemetry) {
    builder =
        DefaultHttpServerInstrumenterBuilder.create(
            INSTRUMENTATION_NAME,
            openTelemetry,
            SpringWebMvcHttpAttributesGetter.INSTANCE,
            JakartaHttpServletRequestGetter.INSTANCE);
  }

  /**
   * Adds an additional {@link AttributesExtractor} to invoke to set attributes to instrumented
   * items.
   */
  @Override
  @CanIgnoreReturnValue
  public SpringWebMvcTelemetryBuilder addAttributesExtractor(
      AttributesExtractor<HttpServletRequest, HttpServletResponse> attributesExtractor) {
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
  public SpringWebMvcTelemetryBuilder setCapturedRequestHeaders(List<String> requestHeaders) {
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
  public SpringWebMvcTelemetryBuilder setCapturedResponseHeaders(List<String> responseHeaders) {
    builder.setCapturedResponseHeaders(responseHeaders);
    return this;
  }

  /** Sets custom {@link SpanNameExtractor} via transform function. */
  @Override
  @CanIgnoreReturnValue
  public SpringWebMvcTelemetryBuilder setSpanNameExtractor(
      Function<SpanNameExtractor<HttpServletRequest>, SpanNameExtractor<HttpServletRequest>>
          spanNameExtractor) {
    builder.setSpanNameExtractor(spanNameExtractor);
    return this;
  }

  @Override
  public SpringWebMvcTelemetryBuilder setStatusExtractor(
      Function<
              SpanStatusExtractor<HttpServletRequest, HttpServletResponse>,
              SpanStatusExtractor<HttpServletRequest, HttpServletResponse>>
          statusExtractorTransformer) {
    builder.setStatusExtractor(statusExtractorTransformer);
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
  public SpringWebMvcTelemetryBuilder setKnownMethods(Set<String> knownMethods) {
    builder.setKnownMethods(knownMethods);
    return this;
  }

  /**
   * Configures the instrumentation to emit experimental HTTP server metrics.
   *
   * @param emitExperimentalHttpServerMetrics {@code true} if the experimental HTTP server metrics
   *     are to be emitted.
   * @deprecated Use {@link Experimental#setEmitExperimentalTelemetry(SpringWebMvcTelemetryBuilder,
   *     boolean)} instead.
   */
  @Deprecated
  @CanIgnoreReturnValue
  public SpringWebMvcTelemetryBuilder setEmitExperimentalHttpServerMetrics(
      boolean emitExperimentalHttpServerMetrics) {
    builder.setEmitExperimentalHttpServerMetrics(emitExperimentalHttpServerMetrics);
    return this;
  }

  /**
   * Returns a new {@link SpringWebMvcTelemetry} with the settings of this {@link
   * SpringWebMvcTelemetryBuilder}.
   */
  @Override
  public SpringWebMvcTelemetry build() {
    return new SpringWebMvcTelemetry(builder.build());
  }
}
