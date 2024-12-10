/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.spring.webflux.v5_3;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.incubator.builder.internal.DefaultHttpServerInstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanStatusExtractor;
import io.opentelemetry.instrumentation.api.semconv.http.HttpServerAttributesExtractorBuilder;
import io.opentelemetry.instrumentation.api.semconv.http.HttpServerTelemetryBuilder;
import io.opentelemetry.instrumentation.spring.webflux.v5_3.internal.Experimental;
import io.opentelemetry.instrumentation.spring.webflux.v5_3.internal.SpringWebfluxBuilderUtil;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.springframework.web.server.ServerWebExchange;

/** A builder of {@link SpringWebfluxServerTelemetry}. */
public final class SpringWebfluxServerTelemetryBuilder
    implements HttpServerTelemetryBuilder<ServerWebExchange, ServerWebExchange> {

  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.spring-webflux-5.3";

  private final DefaultHttpServerInstrumenterBuilder<ServerWebExchange, ServerWebExchange> builder;

  static {
    SpringWebfluxBuilderUtil.setServerBuilderExtractor(builder -> builder.builder);
  }

  SpringWebfluxServerTelemetryBuilder(OpenTelemetry openTelemetry) {
    builder =
        DefaultHttpServerInstrumenterBuilder.create(
            INSTRUMENTATION_NAME,
            openTelemetry,
            WebfluxServerHttpAttributesGetter.INSTANCE,
            WebfluxTextMapGetter.INSTANCE);
  }

  /**
   * Adds an additional {@link AttributesExtractor} to invoke to set attributes to instrumented
   * items.
   */
  @Override
  @CanIgnoreReturnValue
  public SpringWebfluxServerTelemetryBuilder addAttributesExtractor(
      AttributesExtractor<ServerWebExchange, ServerWebExchange> attributesExtractor) {
    builder.addAttributesExtractor(attributesExtractor);
    return this;
  }

  /**
   * Configures the HTTP request headers that will be captured as span attributes from server
   * instrumentation.
   *
   * @param requestHeaders A list of HTTP header names.
   */
  @Override
  @CanIgnoreReturnValue
  public SpringWebfluxServerTelemetryBuilder setCapturedRequestHeaders(
      List<String> requestHeaders) {
    builder.setCapturedRequestHeaders(requestHeaders);
    return this;
  }

  /**
   * Configures the HTTP response headers that will be captured as span attributes from server
   * instrumentation.
   *
   * @param responseHeaders A list of HTTP header names.
   */
  @Override
  @CanIgnoreReturnValue
  public SpringWebfluxServerTelemetryBuilder setCapturedResponseHeaders(
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
   * @see HttpServerAttributesExtractorBuilder#setKnownMethods(Set)
   */
  @Override
  @CanIgnoreReturnValue
  public SpringWebfluxServerTelemetryBuilder setKnownMethods(Set<String> knownMethods) {
    builder.setKnownMethods(knownMethods);
    return this;
  }

  /** Sets custom server {@link SpanNameExtractor} via transform function. */
  @Override
  @CanIgnoreReturnValue
  public SpringWebfluxServerTelemetryBuilder setSpanNameExtractor(
      Function<SpanNameExtractor<ServerWebExchange>, SpanNameExtractor<ServerWebExchange>>
          serverSpanNameExtractor) {
    builder.setSpanNameExtractor(serverSpanNameExtractor);
    return this;
  }

  @Override
  public SpringWebfluxServerTelemetryBuilder setStatusExtractor(
      Function<
              SpanStatusExtractor<ServerWebExchange, ServerWebExchange>,
              SpanStatusExtractor<ServerWebExchange, ServerWebExchange>>
          statusExtractorTransformer) {
    builder.setStatusExtractor(statusExtractorTransformer);
    return this;
  }

  /**
   * Can be used via the unstable method {@link
   * Experimental#setEmitExperimentalTelemetry(SpringWebfluxServerTelemetryBuilder, boolean)}.
   */
  void setEmitExperimentalHttpServerTelemetry(boolean emitExperimentalHttpServerTelemetry) {
    builder.setEmitExperimentalHttpServerMetrics(emitExperimentalHttpServerTelemetry);
  }

  /**
   * Returns a new {@link SpringWebfluxTelemetry} with the settings of this {@link
   * SpringWebfluxServerTelemetryBuilder}.
   */
  @Override
  public SpringWebfluxServerTelemetry build() {
    return new SpringWebfluxServerTelemetry(builder.build());
  }
}
