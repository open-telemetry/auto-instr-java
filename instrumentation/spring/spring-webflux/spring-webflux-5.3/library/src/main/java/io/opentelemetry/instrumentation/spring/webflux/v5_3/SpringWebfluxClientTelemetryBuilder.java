/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.spring.webflux.v5_3;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.incubator.builder.internal.DefaultHttpClientInstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanStatusExtractor;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientAttributesExtractorBuilder;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientTelemetryBuilder;
import io.opentelemetry.instrumentation.spring.webflux.v5_3.internal.Experimental;
import io.opentelemetry.instrumentation.spring.webflux.v5_3.internal.SpringWebfluxBuilderUtil;
import io.opentelemetry.instrumentation.spring.webflux.v5_3.internal.WebClientHttpAttributesGetter;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;

/** A builder of {@link SpringWebfluxClientTelemetry}. */
public final class SpringWebfluxClientTelemetryBuilder
    implements HttpClientTelemetryBuilder<ClientRequest, ClientResponse> {

  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.spring-webflux-5.3";

  private final DefaultHttpClientInstrumenterBuilder<ClientRequest, ClientResponse> builder;
  private final OpenTelemetry openTelemetry;

  static {
    SpringWebfluxBuilderUtil.setClientBuilderExtractor(builder -> builder.builder);
  }

  SpringWebfluxClientTelemetryBuilder(OpenTelemetry openTelemetry) {
    builder =
        DefaultHttpClientInstrumenterBuilder.create(
            INSTRUMENTATION_NAME, openTelemetry, WebClientHttpAttributesGetter.INSTANCE);
    this.openTelemetry = openTelemetry;
  }

  /**
   * Adds an additional {@link AttributesExtractor} to invoke to set attributes to instrumented
   * items for WebClient.
   */
  @Override
  @CanIgnoreReturnValue
  public SpringWebfluxClientTelemetryBuilder addAttributesExtractor(
      AttributesExtractor<ClientRequest, ClientResponse> attributesExtractor) {
    builder.addAttributesExtractor(attributesExtractor);
    return this;
  }

  /**
   * Configures the HTTP WebClient request headers that will be captured as span attributes.
   *
   * @param requestHeaders A list of HTTP header names.
   */
  @Override
  @CanIgnoreReturnValue
  public SpringWebfluxClientTelemetryBuilder setCapturedRequestHeaders(
      List<String> requestHeaders) {
    builder.setCapturedRequestHeaders(requestHeaders);
    return this;
  }

  /**
   * Configures the HTTP WebClient response headers that will be captured as span attributes.
   *
   * @param responseHeaders A list of HTTP header names.
   */
  @Override
  @CanIgnoreReturnValue
  public SpringWebfluxClientTelemetryBuilder setCapturedResponseHeaders(
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
   * @see HttpClientAttributesExtractorBuilder#setKnownMethods(Set)
   */
  @Override
  @CanIgnoreReturnValue
  public SpringWebfluxClientTelemetryBuilder setKnownMethods(Set<String> knownMethods) {
    builder.setKnownMethods(knownMethods);
    return this;
  }

  /** Sets custom client {@link SpanNameExtractor} via transform function. */
  @Override
  @CanIgnoreReturnValue
  public SpringWebfluxClientTelemetryBuilder setSpanNameExtractor(
      Function<SpanNameExtractor<ClientRequest>, SpanNameExtractor<ClientRequest>>
          clientSpanNameExtractor) {
    builder.setSpanNameExtractor(clientSpanNameExtractor);
    return this;
  }

  @Override
  public SpringWebfluxClientTelemetryBuilder setStatusExtractor(
      Function<
              SpanStatusExtractor<ClientRequest, ClientResponse>,
              SpanStatusExtractor<ClientRequest, ClientResponse>>
          statusExtractorTransformer) {
    builder.setStatusExtractor(statusExtractorTransformer);
    return this;
  }

  /**
   * Can be used via the unstable method {@link
   * Experimental#setEmitExperimentalTelemetry(SpringWebfluxClientTelemetryBuilder, boolean)}.
   */
  void setEmitExperimentalHttpClientTelemetry(boolean emitExperimentalHttpClientTelemetry) {
    builder.setEmitExperimentalHttpClientMetrics(emitExperimentalHttpClientTelemetry);
  }

  /**
   * Returns a new {@link SpringWebfluxClientTelemetry} with the settings of this {@link
   * SpringWebfluxClientTelemetryBuilder}.
   */
  @Override
  public SpringWebfluxClientTelemetry build() {
    return new SpringWebfluxClientTelemetry(builder.build(), openTelemetry.getPropagators());
  }
}
