/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.netty.v4_1;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.netty.handler.codec.http.HttpResponse;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.incubator.builder.internal.DefaultHttpClientInstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientAttributesExtractorBuilder;
import io.opentelemetry.instrumentation.netty.common.v4_0.HttpRequestAndChannel;
import io.opentelemetry.instrumentation.netty.common.v4_0.internal.client.NettyClientInstrumenterBuilderFactory;
import io.opentelemetry.instrumentation.netty.common.v4_0.internal.client.NettyClientInstrumenterFactory;
import io.opentelemetry.instrumentation.netty.common.v4_0.internal.client.NettyConnectionInstrumentationFlag;
import io.opentelemetry.instrumentation.netty.v4_1.internal.Experimental;
import java.util.Collection;
import java.util.function.Function;

/** A builder of {@link NettyClientTelemetry}. */
public final class NettyClientTelemetryBuilder {

  private final DefaultHttpClientInstrumenterBuilder<HttpRequestAndChannel, HttpResponse> builder;
  private boolean emitExperimentalHttpClientEvents = false;

  static {
    Experimental.internalSetEmitExperimentalClientTelemetry(
        (builder, emit) -> {
          builder.builder.setEmitExperimentalHttpClientMetrics(emit);
          builder.emitExperimentalHttpClientEvents = emit;
        });
  }

  NettyClientTelemetryBuilder(OpenTelemetry openTelemetry) {
    builder =
        NettyClientInstrumenterBuilderFactory.create("io.opentelemetry.netty-4.1", openTelemetry);
  }

  /**
   * @deprecated Use {@link Experimental#setEmitExperimentalTelemetry(NettyClientTelemetryBuilder,
   *     boolean)} instead.
   */
  @Deprecated
  @CanIgnoreReturnValue
  public NettyClientTelemetryBuilder setEmitExperimentalHttpClientEvents(
      boolean emitExperimentalHttpClientEvents) {
    this.emitExperimentalHttpClientEvents = emitExperimentalHttpClientEvents;
    return this;
  }

  /**
   * Configures the HTTP request headers that will be captured as span attributes.
   *
   * @param capturedRequestHeaders A list of HTTP header names.
   */
  @CanIgnoreReturnValue
  public NettyClientTelemetryBuilder setCapturedRequestHeaders(
      Collection<String> capturedRequestHeaders) {
    builder.setCapturedRequestHeaders(capturedRequestHeaders);
    return this;
  }

  /**
   * Configures the HTTP response headers that will be captured as span attributes.
   *
   * @param capturedResponseHeaders A list of HTTP header names.
   */
  @CanIgnoreReturnValue
  public NettyClientTelemetryBuilder setCapturedResponseHeaders(
      Collection<String> capturedResponseHeaders) {
    builder.setCapturedResponseHeaders(capturedResponseHeaders);
    return this;
  }

  /**
   * Adds an additional {@link AttributesExtractor} to invoke to set attributes to instrumented
   * items.
   */
  @CanIgnoreReturnValue
  public NettyClientTelemetryBuilder addAttributesExtractor(
      AttributesExtractor<HttpRequestAndChannel, HttpResponse> attributesExtractor) {
    builder.addAttributesExtractor(attributesExtractor);
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
  public NettyClientTelemetryBuilder setKnownMethods(Collection<String> knownMethods) {
    builder.setKnownMethods(knownMethods);
    return this;
  }

  /**
   * Configures the instrumentation to emit experimental HTTP client metrics.
   *
   * @param emitExperimentalHttpClientMetrics {@code true} if the experimental HTTP client metrics
   *     are to be emitted.
   * @deprecated Use {@link Experimental#setEmitExperimentalTelemetry(NettyClientTelemetryBuilder,
   *     boolean)} instead.
   */
  @Deprecated
  @CanIgnoreReturnValue
  public NettyClientTelemetryBuilder setEmitExperimentalHttpClientMetrics(
      boolean emitExperimentalHttpClientMetrics) {
    builder.setEmitExperimentalHttpClientMetrics(emitExperimentalHttpClientMetrics);
    return this;
  }

  /** Sets custom {@link SpanNameExtractor} via transform function. */
  @CanIgnoreReturnValue
  public NettyClientTelemetryBuilder setSpanNameExtractor(
      Function<
              SpanNameExtractor<? super HttpRequestAndChannel>,
              ? extends SpanNameExtractor<? super HttpRequestAndChannel>>
          spanNameExtractorTransformer) {
    builder.setSpanNameExtractor(spanNameExtractorTransformer);
    return this;
  }

  /** Returns a new {@link NettyClientTelemetry} with the given configuration. */
  public NettyClientTelemetry build() {
    return new NettyClientTelemetry(
        new NettyClientInstrumenterFactory(
                builder,
                NettyConnectionInstrumentationFlag.DISABLED,
                NettyConnectionInstrumentationFlag.DISABLED)
            .instrumenter(),
        emitExperimentalHttpClientEvents);
  }
}
