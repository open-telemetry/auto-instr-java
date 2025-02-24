/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.incubator.builder.internal;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.instrumentation.api.incubator.config.internal.CommonConfig;
import io.opentelemetry.instrumentation.api.incubator.semconv.http.HttpClientExperimentalMetrics;
import io.opentelemetry.instrumentation.api.incubator.semconv.http.HttpClientPeerServiceAttributesExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.http.HttpExperimentalAttributesExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.net.PeerServiceResolver;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanStatusExtractor;
import io.opentelemetry.instrumentation.api.internal.Experimental;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientAttributesExtractorBuilder;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientAttributesGetter;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientMetrics;
import io.opentelemetry.instrumentation.api.semconv.http.HttpSpanNameExtractor;
import io.opentelemetry.instrumentation.api.semconv.http.HttpSpanNameExtractorBuilder;
import io.opentelemetry.instrumentation.api.semconv.http.HttpSpanStatusExtractor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.annotation.Nullable;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class DefaultHttpClientInstrumenterBuilder<REQUEST, RESPONSE> {

  // copied from PeerIncubatingAttributes
  private static final AttributeKey<String> PEER_SERVICE = AttributeKey.stringKey("peer.service");

  private final String instrumentationName;
  private final OpenTelemetry openTelemetry;

  private final List<AttributesExtractor<? super REQUEST, ? super RESPONSE>> additionalExtractors =
      new ArrayList<>();
  private Function<
          SpanStatusExtractor<? super REQUEST, ? super RESPONSE>,
          ? extends SpanStatusExtractor<? super REQUEST, ? super RESPONSE>>
      statusExtractorTransformer = Function.identity();
  private final HttpClientAttributesExtractorBuilder<REQUEST, RESPONSE>
      httpAttributesExtractorBuilder;
  private final HttpClientAttributesGetter<REQUEST, RESPONSE> attributesGetter;
  private final HttpSpanNameExtractorBuilder<REQUEST> httpSpanNameExtractorBuilder;

  @Nullable private final TextMapSetter<REQUEST> headerSetter;
  private Function<SpanNameExtractor<? super REQUEST>, ? extends SpanNameExtractor<? super REQUEST>>
      spanNameExtractorTransformer = Function.identity();
  private boolean emitExperimentalHttpClientMetrics = false;
  private Consumer<InstrumenterBuilder<REQUEST, RESPONSE>> builderCustomizer = b -> {};

  private DefaultHttpClientInstrumenterBuilder(
      String instrumentationName,
      OpenTelemetry openTelemetry,
      HttpClientAttributesGetter<REQUEST, RESPONSE> attributesGetter,
      TextMapSetter<REQUEST> headerSetter) {
    this.instrumentationName = Objects.requireNonNull(instrumentationName, "instrumentationName");
    this.openTelemetry = Objects.requireNonNull(openTelemetry, "openTelemetry");
    this.attributesGetter = Objects.requireNonNull(attributesGetter, "attributesGetter");
    httpSpanNameExtractorBuilder = HttpSpanNameExtractor.builder(attributesGetter);
    httpAttributesExtractorBuilder = HttpClientAttributesExtractor.builder(attributesGetter);
    this.headerSetter = headerSetter;
  }

  public static <REQUEST, RESPONSE> DefaultHttpClientInstrumenterBuilder<REQUEST, RESPONSE> create(
      String instrumentationName,
      OpenTelemetry openTelemetry,
      HttpClientAttributesGetter<REQUEST, RESPONSE> attributesGetter) {
    return new DefaultHttpClientInstrumenterBuilder<REQUEST, RESPONSE>(
        instrumentationName, openTelemetry, attributesGetter, null);
  }

  public static <REQUEST, RESPONSE> DefaultHttpClientInstrumenterBuilder<REQUEST, RESPONSE> create(
      String instrumentationName,
      OpenTelemetry openTelemetry,
      HttpClientAttributesGetter<REQUEST, RESPONSE> attributesGetter,
      TextMapSetter<REQUEST> headerSetter) {
    return new DefaultHttpClientInstrumenterBuilder<REQUEST, RESPONSE>(
        instrumentationName,
        openTelemetry,
        attributesGetter,
        Objects.requireNonNull(headerSetter, "headerSetter"));
  }

  /**
   * Adds an additional {@link AttributesExtractor} to invoke to set attributes to instrumented
   * items. The {@link AttributesExtractor} will be executed after all default extractors.
   */
  @CanIgnoreReturnValue
  public DefaultHttpClientInstrumenterBuilder<REQUEST, RESPONSE> addAttributesExtractor(
      AttributesExtractor<? super REQUEST, ? super RESPONSE> attributesExtractor) {
    additionalExtractors.add(attributesExtractor);
    return this;
  }

  @CanIgnoreReturnValue
  public DefaultHttpClientInstrumenterBuilder<REQUEST, RESPONSE> setStatusExtractor(
      Function<
              SpanStatusExtractor<? super REQUEST, ? super RESPONSE>,
              ? extends SpanStatusExtractor<? super REQUEST, ? super RESPONSE>>
          statusExtractor) {
    this.statusExtractorTransformer = statusExtractor;
    return this;
  }

  /**
   * Configures the HTTP request headers that will be captured as span attributes.
   *
   * @param requestHeaders A list of HTTP header names.
   */
  @CanIgnoreReturnValue
  public DefaultHttpClientInstrumenterBuilder<REQUEST, RESPONSE> setCapturedRequestHeaders(
      Collection<String> requestHeaders) {
    httpAttributesExtractorBuilder.setCapturedRequestHeaders(requestHeaders);
    return this;
  }

  /**
   * Configures the HTTP response headers that will be captured as span attributes.
   *
   * @param responseHeaders A list of HTTP header names.
   */
  @CanIgnoreReturnValue
  public DefaultHttpClientInstrumenterBuilder<REQUEST, RESPONSE> setCapturedResponseHeaders(
      Collection<String> responseHeaders) {
    httpAttributesExtractorBuilder.setCapturedResponseHeaders(responseHeaders);
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
   * @see HttpClientAttributesExtractorBuilder#setKnownMethods(Collection)
   */
  @CanIgnoreReturnValue
  public DefaultHttpClientInstrumenterBuilder<REQUEST, RESPONSE> setKnownMethods(
      Collection<String> knownMethods) {
    httpAttributesExtractorBuilder.setKnownMethods(knownMethods);
    httpSpanNameExtractorBuilder.setKnownMethods(knownMethods);
    return this;
  }

  /**
   * Configures the instrumentation to emit experimental HTTP client metrics.
   *
   * @param emitExperimentalHttpClientMetrics {@code true} if the experimental HTTP client metrics
   *     are to be emitted.
   */
  @CanIgnoreReturnValue
  public DefaultHttpClientInstrumenterBuilder<REQUEST, RESPONSE>
      setEmitExperimentalHttpClientMetrics(boolean emitExperimentalHttpClientMetrics) {
    this.emitExperimentalHttpClientMetrics = emitExperimentalHttpClientMetrics;
    return this;
  }

  /**
   * Configures the instrumentation to redact sensitive URL parameters.
   *
   * @param redactQueryParameters {@code true} if the sensitive URL parameters have to be redacted.
   */
  @CanIgnoreReturnValue
  public DefaultHttpClientInstrumenterBuilder<REQUEST, RESPONSE> setRedactQueryParameters(
      boolean redactQueryParameters) {
    Experimental.setRedactQueryParameters(httpAttributesExtractorBuilder, redactQueryParameters);
    return this;
  }

  /** Sets custom {@link SpanNameExtractor} via transform function. */
  @CanIgnoreReturnValue
  public DefaultHttpClientInstrumenterBuilder<REQUEST, RESPONSE> setSpanNameExtractor(
      Function<SpanNameExtractor<? super REQUEST>, ? extends SpanNameExtractor<? super REQUEST>>
          spanNameExtractorTransformer) {
    this.spanNameExtractorTransformer = spanNameExtractorTransformer;
    return this;
  }

  /** Sets custom {@link PeerServiceResolver}. */
  @CanIgnoreReturnValue
  public DefaultHttpClientInstrumenterBuilder<REQUEST, RESPONSE> setPeerServiceResolver(
      PeerServiceResolver peerServiceResolver) {
    return addAttributesExtractor(
        HttpClientPeerServiceAttributesExtractor.create(attributesGetter, peerServiceResolver));
  }

  /** Sets the {@code peer.service} attribute for http client spans. */
  @CanIgnoreReturnValue
  public DefaultHttpClientInstrumenterBuilder<REQUEST, RESPONSE> setPeerService(
      String peerService) {
    return addAttributesExtractor(AttributesExtractor.constant(PEER_SERVICE, peerService));
  }

  @CanIgnoreReturnValue
  public DefaultHttpClientInstrumenterBuilder<REQUEST, RESPONSE> setBuilderCustomizer(
      Consumer<InstrumenterBuilder<REQUEST, RESPONSE>> builderCustomizer) {
    this.builderCustomizer = builderCustomizer;
    return this;
  }

  public Instrumenter<REQUEST, RESPONSE> build() {
    SpanNameExtractor<? super REQUEST> spanNameExtractor =
        spanNameExtractorTransformer.apply(httpSpanNameExtractorBuilder.build());

    InstrumenterBuilder<REQUEST, RESPONSE> builder =
        Instrumenter.<REQUEST, RESPONSE>builder(
                openTelemetry, instrumentationName, spanNameExtractor)
            .setSpanStatusExtractor(
                statusExtractorTransformer.apply(HttpSpanStatusExtractor.create(attributesGetter)))
            .addAttributesExtractor(httpAttributesExtractorBuilder.build())
            .addAttributesExtractors(additionalExtractors)
            .addOperationMetrics(HttpClientMetrics.get());
    if (emitExperimentalHttpClientMetrics) {
      builder
          .addAttributesExtractor(HttpExperimentalAttributesExtractor.create(attributesGetter))
          .addOperationMetrics(HttpClientExperimentalMetrics.get());
    }

    builderCustomizer.accept(builder);

    if (headerSetter != null) {
      return builder.buildClientInstrumenter(headerSetter);
    }
    return builder.buildInstrumenter(SpanKindExtractor.alwaysClient());
  }

  public <BUILDERREQUEST, BUILDERRESPONSE>
      InstrumenterBuilder<BUILDERREQUEST, BUILDERRESPONSE> instrumenterBuilder(
          SpanNameExtractor<? super BUILDERREQUEST> spanNameExtractor) {
    return Instrumenter.builder(openTelemetry, instrumentationName, spanNameExtractor);
  }

  @CanIgnoreReturnValue
  public DefaultHttpClientInstrumenterBuilder<REQUEST, RESPONSE> configure(CommonConfig config) {
    set(config::getKnownHttpRequestMethods, this::setKnownMethods);
    set(config::getClientRequestHeaders, this::setCapturedRequestHeaders);
    set(config::getClientResponseHeaders, this::setCapturedResponseHeaders);
    set(config::getPeerServiceResolver, this::setPeerServiceResolver);
    set(
        config::shouldEmitExperimentalHttpClientTelemetry,
        this::setEmitExperimentalHttpClientMetrics);
    set(config::redactQueryParameters, this::setRedactQueryParameters);
    return this;
  }

  private static <T> void set(Supplier<T> supplier, Consumer<T> consumer) {
    T t = supplier.get();
    if (t != null) {
      consumer.accept(t);
    }
  }
}
