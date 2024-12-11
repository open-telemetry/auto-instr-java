/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.armeria.v1_3;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import com.linecorp.armeria.client.ClientRequestContext;
import com.linecorp.armeria.common.logging.RequestLog;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.incubator.builder.internal.DefaultHttpClientInstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanStatusExtractor;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientAttributesExtractorBuilder;
import io.opentelemetry.instrumentation.armeria.v1_3.internal.ArmeriaInstrumenterBuilderFactory;
import io.opentelemetry.instrumentation.armeria.v1_3.internal.ArmeriaInstrumenterBuilderUtil;
import io.opentelemetry.instrumentation.armeria.v1_3.internal.Experimental;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public final class ArmeriaClientTelemetryBuilder {

  private final DefaultHttpClientInstrumenterBuilder<ClientRequestContext, RequestLog> builder;

  static {
    ArmeriaInstrumenterBuilderUtil.setClientBuilderExtractor(builder -> builder.builder);
    Experimental.setSetEmitExperimentalClientTelemetry(
        (builder, emit) -> builder.builder.setEmitExperimentalHttpClientMetrics(emit));
  }

  ArmeriaClientTelemetryBuilder(OpenTelemetry openTelemetry) {
    builder = ArmeriaInstrumenterBuilderFactory.getClientBuilder(openTelemetry);
  }

  /** Sets the status extractor for client spans. */
  @CanIgnoreReturnValue
  public ArmeriaClientTelemetryBuilder setStatusExtractor(
      Function<
              SpanStatusExtractor<? super ClientRequestContext, ? super RequestLog>,
              ? extends SpanStatusExtractor<? super ClientRequestContext, ? super RequestLog>>
          statusExtractor) {
    builder.setStatusExtractor(statusExtractor);
    return this;
  }

  /**
   * Adds an extra {@link AttributesExtractor} to invoke to set attributes to instrumented items.
   * The {@link AttributesExtractor} will be executed after all default extractors.
   */
  @CanIgnoreReturnValue
  public ArmeriaClientTelemetryBuilder addAttributesExtractor(
      AttributesExtractor<? super ClientRequestContext, ? super RequestLog> attributesExtractor) {
    builder.addAttributesExtractor(attributesExtractor);
    return this;
  }

  /**
   * Configures the HTTP client request headers that will be captured as span attributes.
   *
   * @param requestHeaders A list of HTTP header names.
   */
  @CanIgnoreReturnValue
  public ArmeriaClientTelemetryBuilder setCapturedRequestHeaders(List<String> requestHeaders) {
    builder.setCapturedRequestHeaders(requestHeaders);
    return this;
  }

  /**
   * Configures the HTTP client response headers that will be captured as span attributes.
   *
   * @param responseHeaders A list of HTTP header names.
   */
  @CanIgnoreReturnValue
  public ArmeriaClientTelemetryBuilder setCapturedResponseHeaders(List<String> responseHeaders) {
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
  public ArmeriaClientTelemetryBuilder setKnownMethods(Set<String> knownMethods) {
    builder.setKnownMethods(knownMethods);
    return this;
  }

  /** Sets custom client {@link SpanNameExtractor} via transform function. */
  @CanIgnoreReturnValue
  public ArmeriaClientTelemetryBuilder setSpanNameExtractor(
      Function<
              SpanNameExtractor<? super ClientRequestContext>,
              ? extends SpanNameExtractor<? super ClientRequestContext>>
          clientSpanNameExtractor) {
    builder.setSpanNameExtractor(clientSpanNameExtractor);
    return this;
  }

  /**
   * Can be used via the unstable method {@link
   * Experimental#setClientPeerService(ArmeriaClientTelemetryBuilder, String)}.
   */
  void setPeerService(String peerService) {
    builder.setPeerService(peerService);
  }

  /**
   * Can be used via the unstable method {@link
   * Experimental#setEmitExperimentalTelemetry(ArmeriaClientTelemetryBuilder, boolean)}.
   */
  void setEmitExperimentalHttpClientMetrics(boolean emitExperimentalHttpClientMetrics) {
    builder.setEmitExperimentalHttpClientMetrics(emitExperimentalHttpClientMetrics);
  }

  public ArmeriaClientTelemetry build() {
    return new ArmeriaClientTelemetry(builder.build());
  }
}
