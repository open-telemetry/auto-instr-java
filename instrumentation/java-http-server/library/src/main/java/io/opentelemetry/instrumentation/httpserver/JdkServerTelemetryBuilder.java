/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.httpserver;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.sun.net.httpserver.HttpExchange;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.incubator.builder.internal.DefaultHttpServerInstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanStatusExtractor;
import io.opentelemetry.instrumentation.api.semconv.http.HttpServerAttributesExtractorBuilder;
import io.opentelemetry.instrumentation.httpserver.internal.JdkInstrumenterBuilderFactory;
import io.opentelemetry.instrumentation.httpserver.internal.JdkInstrumenterBuilderUtil;
import java.util.Collection;
import java.util.function.Function;

public final class JdkServerTelemetryBuilder {

  private final DefaultHttpServerInstrumenterBuilder<HttpExchange, HttpExchange> builder;

  static {
    JdkInstrumenterBuilderUtil.setServerBuilderExtractor(builder -> builder.builder);
  }

  JdkServerTelemetryBuilder(OpenTelemetry openTelemetry) {
    builder = JdkInstrumenterBuilderFactory.getServerBuilder(openTelemetry);
  }

  /** Sets the status extractor for server spans. */
  @CanIgnoreReturnValue
  public JdkServerTelemetryBuilder setStatusExtractor(
      Function<
              SpanStatusExtractor<? super HttpExchange, ? super HttpExchange>,
              ? extends SpanStatusExtractor<? super HttpExchange, ? super HttpExchange>>
          statusExtractor) {
    builder.setStatusExtractor(statusExtractor);
    return this;
  }

  /**
   * Adds an extra {@link AttributesExtractor} to invoke to set attributes to instrumented items.
   * The {@link AttributesExtractor} will be executed after all default extractors.
   */
  @CanIgnoreReturnValue
  public JdkServerTelemetryBuilder addAttributesExtractor(
      AttributesExtractor<HttpExchange, HttpExchange> attributesExtractor) {
    builder.addAttributesExtractor(attributesExtractor);
    return this;
  }

  /**
   * Configures the HTTP server request headers that will be captured as span attributes.
   *
   * @param requestHeaders A list of HTTP header names.
   */
  @CanIgnoreReturnValue
  public JdkServerTelemetryBuilder setCapturedRequestHeaders(Collection<String> requestHeaders) {
    builder.setCapturedRequestHeaders(requestHeaders);
    return this;
  }

  /**
   * Configures the HTTP server response headers that will be captured as span attributes.
   *
   * @param responseHeaders A list of HTTP header names.
   */
  @CanIgnoreReturnValue
  public JdkServerTelemetryBuilder setCapturedResponseHeaders(Collection<String> responseHeaders) {
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
   * @see HttpServerAttributesExtractorBuilder#setKnownMethods(Collection)
   */
  @CanIgnoreReturnValue
  public JdkServerTelemetryBuilder setKnownMethods(Collection<String> knownMethods) {
    builder.setKnownMethods(knownMethods);
    return this;
  }

  /** Sets custom server {@link SpanNameExtractor} via transform function. */
  @CanIgnoreReturnValue
  public JdkServerTelemetryBuilder setSpanNameExtractor(
      Function<
              SpanNameExtractor<? super HttpExchange>,
              ? extends SpanNameExtractor<? super HttpExchange>>
          serverSpanNameExtractor) {
    builder.setSpanNameExtractor(serverSpanNameExtractor);
    return this;
  }

  public JdkServerTelemetry build() {
    return new JdkServerTelemetry(builder.build());
  }
}
