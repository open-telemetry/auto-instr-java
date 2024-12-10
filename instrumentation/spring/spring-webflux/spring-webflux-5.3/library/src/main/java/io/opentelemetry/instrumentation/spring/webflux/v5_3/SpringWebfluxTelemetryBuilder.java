/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.spring.webflux.v5_3;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.incubator.builder.internal.DefaultHttpClientInstrumenterBuilder;
import io.opentelemetry.instrumentation.api.incubator.builder.internal.DefaultHttpServerInstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientAttributesExtractorBuilder;
import io.opentelemetry.instrumentation.api.semconv.http.HttpServerAttributesExtractorBuilder;
import io.opentelemetry.instrumentation.spring.webflux.v5_3.internal.Experimental;
import io.opentelemetry.instrumentation.spring.webflux.v5_3.internal.WebClientHttpAttributesGetter;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.server.ServerWebExchange;

/**
 * A builder of {@link SpringWebfluxTelemetry}.
 *
 * @deprecated Use {@link SpringWebfluxClientTelemetryBuilder} and {@link
 *     SpringWebfluxServerTelemetryBuilder} instead.
 */
@Deprecated
public final class SpringWebfluxTelemetryBuilder {
  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.spring-webflux-5.3";

  private final DefaultHttpClientInstrumenterBuilder<ClientRequest, ClientResponse> clientBuilder;
  private final DefaultHttpServerInstrumenterBuilder<ServerWebExchange, ServerWebExchange>
      serverBuilder;
  private final OpenTelemetry openTelemetry;

  SpringWebfluxTelemetryBuilder(OpenTelemetry openTelemetry) {
    clientBuilder =
        DefaultHttpClientInstrumenterBuilder.create(
            INSTRUMENTATION_NAME, openTelemetry, WebClientHttpAttributesGetter.INSTANCE);
    serverBuilder =
        DefaultHttpServerInstrumenterBuilder.create(
            INSTRUMENTATION_NAME,
            openTelemetry,
            WebfluxServerHttpAttributesGetter.INSTANCE,
            WebfluxTextMapGetter.INSTANCE);
    this.openTelemetry = openTelemetry;
  }

  /**
   * Adds an additional {@link AttributesExtractor} to invoke to set attributes to instrumented
   * items for WebClient.
   *
   * @deprecated Use {@link
   *     SpringWebfluxClientTelemetryBuilder#addAttributesExtractor(AttributesExtractor)} instead.
   */
  @Deprecated
  @CanIgnoreReturnValue
  public SpringWebfluxTelemetryBuilder addClientAttributesExtractor(
      AttributesExtractor<ClientRequest, ClientResponse> attributesExtractor) {
    clientBuilder.addAttributesExtractor(attributesExtractor);
    return this;
  }

  /**
   * Configures the HTTP WebClient request headers that will be captured as span attributes.
   *
   * @param requestHeaders A list of HTTP header names.
   * @deprecated Use {@link SpringWebfluxClientTelemetryBuilder#setCapturedRequestHeaders(List)}
   *     instead.
   */
  @Deprecated
  @CanIgnoreReturnValue
  public SpringWebfluxTelemetryBuilder setCapturedClientRequestHeaders(
      List<String> requestHeaders) {
    clientBuilder.setCapturedRequestHeaders(requestHeaders);
    return this;
  }

  /**
   * Configures the HTTP WebClient response headers that will be captured as span attributes.
   *
   * @param responseHeaders A list of HTTP header names.
   * @deprecated Use {@link SpringWebfluxClientTelemetryBuilder#setCapturedResponseHeaders(List)}
   *     instead.
   */
  @Deprecated
  @CanIgnoreReturnValue
  public SpringWebfluxTelemetryBuilder setCapturedClientResponseHeaders(
      List<String> responseHeaders) {
    clientBuilder.setCapturedResponseHeaders(responseHeaders);
    return this;
  }

  /**
   * Adds an additional {@link AttributesExtractor} to invoke to set attributes to instrumented
   * items.
   *
   * @deprecated Use {@link
   *     SpringWebfluxServerTelemetryBuilder#addAttributesExtractor(AttributesExtractor)} instead.
   */
  @Deprecated
  @CanIgnoreReturnValue
  public SpringWebfluxTelemetryBuilder addServerAttributesExtractor(
      AttributesExtractor<ServerWebExchange, ServerWebExchange> attributesExtractor) {
    serverBuilder.addAttributesExtractor(attributesExtractor);
    return this;
  }

  /**
   * Configures the HTTP request headers that will be captured as span attributes from server
   * instrumentation.
   *
   * @param requestHeaders A list of HTTP header names.
   * @deprecated Use {@link SpringWebfluxServerTelemetryBuilder#setCapturedRequestHeaders(List)}
   *     instead.
   */
  @Deprecated
  @CanIgnoreReturnValue
  public SpringWebfluxTelemetryBuilder setCapturedServerRequestHeaders(
      List<String> requestHeaders) {
    serverBuilder.setCapturedRequestHeaders(requestHeaders);
    return this;
  }

  /**
   * Configures the HTTP response headers that will be captured as span attributes from server
   * instrumentation.
   *
   * @param responseHeaders A list of HTTP header names.
   * @deprecated Use {@link SpringWebfluxServerTelemetryBuilder#setCapturedResponseHeaders(List)}
   *     instead.
   */
  @Deprecated
  @CanIgnoreReturnValue
  public SpringWebfluxTelemetryBuilder setCapturedServerResponseHeaders(
      List<String> responseHeaders) {
    serverBuilder.setCapturedResponseHeaders(responseHeaders);
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
   * @see HttpServerAttributesExtractorBuilder#setKnownMethods(Set)
   * @deprecated Use {@link SpringWebfluxClientTelemetryBuilder#setKnownMethods(Set)} and {@link
   *     SpringWebfluxServerTelemetryBuilder#setKnownMethods(Set)} instead.
   */
  @Deprecated
  @CanIgnoreReturnValue
  public SpringWebfluxTelemetryBuilder setKnownMethods(Set<String> knownMethods) {
    clientBuilder.setKnownMethods(knownMethods);
    serverBuilder.setKnownMethods(knownMethods);
    return this;
  }

  /**
   * Configures the instrumentation to emit experimental HTTP client metrics.
   *
   * @param emitExperimentalHttpClientTelemetry {@code true} if the experimental HTTP client metrics
   *     are to be emitted.
   * @deprecated Use {@link
   *     Experimental#setEmitExperimentalTelemetry(SpringWebfluxClientTelemetryBuilder, boolean)}
   *     instead.
   */
  @Deprecated
  @CanIgnoreReturnValue
  public SpringWebfluxTelemetryBuilder setEmitExperimentalHttpClientTelemetry(
      boolean emitExperimentalHttpClientTelemetry) {
    clientBuilder.setEmitExperimentalHttpClientMetrics(emitExperimentalHttpClientTelemetry);
    return this;
  }

  /**
   * Configures the instrumentation to emit experimental HTTP server metrics.
   *
   * @param emitExperimentalHttpServerTelemetry {@code true} if the experimental HTTP server metrics
   *     are to be emitted.
   * @deprecated Use {@link
   *     Experimental#setEmitExperimentalTelemetry(SpringWebfluxServerTelemetryBuilder, boolean)}
   *     instead.
   */
  @Deprecated
  @CanIgnoreReturnValue
  public SpringWebfluxTelemetryBuilder setEmitExperimentalHttpServerTelemetry(
      boolean emitExperimentalHttpServerTelemetry) {
    serverBuilder.setEmitExperimentalHttpServerMetrics(emitExperimentalHttpServerTelemetry);
    return this;
  }

  /**
   * Sets custom client {@link SpanNameExtractor} via transform function.
   *
   * @deprecated Use {@link SpringWebfluxClientTelemetryBuilder#setSpanNameExtractor(Function)}
   *     instead.
   */
  @Deprecated
  @CanIgnoreReturnValue
  public SpringWebfluxTelemetryBuilder setClientSpanNameExtractor(
      Function<SpanNameExtractor<ClientRequest>, SpanNameExtractor<ClientRequest>>
          clientSpanNameExtractor) {
    clientBuilder.setSpanNameExtractor(clientSpanNameExtractor);
    return this;
  }

  /**
   * Sets custom server {@link SpanNameExtractor} via transform function.
   *
   * @deprecated Use {@link SpringWebfluxServerTelemetryBuilder#setSpanNameExtractor(Function)}
   *     instead.
   */
  @Deprecated
  @CanIgnoreReturnValue
  public SpringWebfluxTelemetryBuilder setServerSpanNameExtractor(
      Function<SpanNameExtractor<ServerWebExchange>, SpanNameExtractor<ServerWebExchange>>
          serverSpanNameExtractor) {
    serverBuilder.setSpanNameExtractor(serverSpanNameExtractor);
    return this;
  }

  /**
   * Returns a new {@link SpringWebfluxTelemetry} with the settings of this {@link
   * SpringWebfluxTelemetryBuilder}.
   *
   * @deprecated Use {@link SpringWebfluxClientTelemetryBuilder#build()} and {@link
   *     SpringWebfluxServerTelemetryBuilder#build()} instead.
   */
  @Deprecated
  public SpringWebfluxTelemetry build() {
    return new SpringWebfluxTelemetry(
        clientBuilder.build(), serverBuilder.build(), openTelemetry.getPropagators());
  }
}
