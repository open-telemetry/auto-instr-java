/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.armeria.v1_3;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.linecorp.armeria.common.logging.RequestLog;
import com.linecorp.armeria.server.ServiceRequestContext;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.incubator.builder.internal.DefaultHttpServerInstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanStatusExtractor;
import io.opentelemetry.instrumentation.api.semconv.http.HttpServerAttributesExtractorBuilder;
import io.opentelemetry.instrumentation.api.semconv.http.HttpServerTelemetryBuilder;
import io.opentelemetry.instrumentation.armeria.v1_3.internal.ArmeriaInstrumenterBuilderFactory;
import io.opentelemetry.instrumentation.armeria.v1_3.internal.ArmeriaInstrumenterBuilderUtil;
import io.opentelemetry.instrumentation.armeria.v1_3.internal.Experimental;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public final class ArmeriaServerTelemetryBuilder
    implements HttpServerTelemetryBuilder<ServiceRequestContext, RequestLog> {

  private final DefaultHttpServerInstrumenterBuilder<ServiceRequestContext, RequestLog> builder;

  static {
    ArmeriaInstrumenterBuilderUtil.setServerBuilderExtractor(builder -> builder.builder);
  }

  ArmeriaServerTelemetryBuilder(OpenTelemetry openTelemetry) {
    builder = ArmeriaInstrumenterBuilderFactory.getServerBuilder(openTelemetry);
  }

  /** Sets the status extractor for server spans. */
  @Override
  @CanIgnoreReturnValue
  public ArmeriaServerTelemetryBuilder setStatusExtractor(
      Function<
              SpanStatusExtractor<ServiceRequestContext, RequestLog>,
              SpanStatusExtractor<ServiceRequestContext, RequestLog>>
          statusExtractor) {
    builder.setStatusExtractor(statusExtractor);
    return this;
  }

  /**
   * Adds an extra {@link AttributesExtractor} to invoke to set attributes to instrumented items.
   * The {@link AttributesExtractor} will be executed after all default extractors.
   */
  @Override
  @CanIgnoreReturnValue
  public ArmeriaServerTelemetryBuilder addAttributesExtractor(
      AttributesExtractor<ServiceRequestContext, RequestLog> attributesExtractor) {
    builder.addAttributesExtractor(attributesExtractor);
    return this;
  }

  /**
   * Configures the HTTP server request headers that will be captured as span attributes.
   *
   * @param requestHeaders A list of HTTP header names.
   */
  @Override
  @CanIgnoreReturnValue
  public ArmeriaServerTelemetryBuilder setCapturedRequestHeaders(List<String> requestHeaders) {
    builder.setCapturedRequestHeaders(requestHeaders);
    return this;
  }

  /**
   * Configures the HTTP server response headers that will be captured as span attributes.
   *
   * @param responseHeaders A list of HTTP header names.
   */
  @Override
  @CanIgnoreReturnValue
  public ArmeriaServerTelemetryBuilder setCapturedResponseHeaders(List<String> responseHeaders) {
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
  public ArmeriaServerTelemetryBuilder setKnownMethods(Set<String> knownMethods) {
    builder.setKnownMethods(knownMethods);
    return this;
  }

  /** Sets custom server {@link SpanNameExtractor} via transform function. */
  @Override
  @CanIgnoreReturnValue
  public ArmeriaServerTelemetryBuilder setSpanNameExtractor(
      Function<SpanNameExtractor<ServiceRequestContext>, SpanNameExtractor<ServiceRequestContext>>
          serverSpanNameExtractor) {
    builder.setSpanNameExtractor(serverSpanNameExtractor);
    return this;
  }

  /**
   * Can be used via the unstable method {@link
   * Experimental#setEmitExperimentalTelemetry(ArmeriaServerTelemetryBuilder, boolean)}.
   */
  void setEmitExperimentalHttpServerMetrics(boolean emitExperimentalHttpServerMetrics) {
    builder.setEmitExperimentalHttpServerMetrics(emitExperimentalHttpServerMetrics);
  }

  @Override
  public ArmeriaServerTelemetry build() {
    return new ArmeriaServerTelemetry(builder.build());
  }
}
