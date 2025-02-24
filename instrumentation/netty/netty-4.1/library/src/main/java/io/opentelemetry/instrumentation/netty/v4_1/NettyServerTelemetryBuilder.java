/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.netty.v4_1;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.netty.handler.codec.http.HttpResponse;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.incubator.builder.internal.DefaultHttpServerInstrumenterBuilder;
import io.opentelemetry.instrumentation.api.semconv.http.HttpServerAttributesExtractorBuilder;
import io.opentelemetry.instrumentation.netty.common.v4_0.HttpRequestAndChannel;
import io.opentelemetry.instrumentation.netty.common.v4_0.internal.server.HttpRequestHeadersGetter;
import io.opentelemetry.instrumentation.netty.common.v4_0.internal.server.NettyHttpServerAttributesGetter;
import io.opentelemetry.instrumentation.netty.v4_1.internal.Experimental;
import io.opentelemetry.instrumentation.netty.v4_1.internal.ProtocolEventHandler;
import io.opentelemetry.instrumentation.netty.v4_1.internal.server.NettyServerInstrumenterBuilderUtil;
import java.util.Collection;

/** A builder of {@link NettyServerTelemetry}. */
public final class NettyServerTelemetryBuilder {

  private final DefaultHttpServerInstrumenterBuilder<HttpRequestAndChannel, HttpResponse> builder;

  private boolean emitExperimentalHttpServerEvents = false;

  static {
    NettyServerInstrumenterBuilderUtil.setBuilderExtractor(
        nettyServerTelemetryBuilder -> nettyServerTelemetryBuilder.builder);
    Experimental.internalSetEmitExperimentalServerTelemetry(
        (builder, emit) -> {
          builder.builder.setEmitExperimentalHttpServerMetrics(emit);
          builder.emitExperimentalHttpServerEvents = emit;
        });
  }

  NettyServerTelemetryBuilder(OpenTelemetry openTelemetry) {
    builder =
        DefaultHttpServerInstrumenterBuilder.create(
            "io.opentelemetry.netty-4.1",
            openTelemetry,
            new NettyHttpServerAttributesGetter(),
            HttpRequestHeadersGetter.INSTANCE);
  }

  /**
   * Configures emission of experimental events.
   *
   * @param emitExperimentalHttpServerEvents set to true to emit events
   */
  @CanIgnoreReturnValue
  public NettyServerTelemetryBuilder setEmitExperimentalHttpServerEvents(
      boolean emitExperimentalHttpServerEvents) {
    this.emitExperimentalHttpServerEvents = emitExperimentalHttpServerEvents;
    return this;
  }

  /**
   * Configures the HTTP request headers that will be captured as span attributes.
   *
   * @param capturedRequestHeaders A list of HTTP header names.
   */
  @CanIgnoreReturnValue
  public NettyServerTelemetryBuilder setCapturedRequestHeaders(
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
  public NettyServerTelemetryBuilder setCapturedResponseHeaders(
      Collection<String> capturedResponseHeaders) {
    builder.setCapturedResponseHeaders(capturedResponseHeaders);
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
  public NettyServerTelemetryBuilder setKnownMethods(Collection<String> knownMethods) {
    builder.setKnownMethods(knownMethods);
    return this;
  }

  /**
   * Configures the instrumentation to emit experimental HTTP server metrics.
   *
   * @param emitExperimentalHttpServerMetrics {@code true} if the experimental HTTP server metrics
   *     are to be emitted.
   * @deprecated Use {@link Experimental#setEmitExperimentalTelemetry(NettyServerTelemetryBuilder,
   *     boolean)} instead.
   */
  @Deprecated
  @CanIgnoreReturnValue
  public NettyServerTelemetryBuilder setEmitExperimentalHttpServerMetrics(
      boolean emitExperimentalHttpServerMetrics) {
    builder.setEmitExperimentalHttpServerMetrics(emitExperimentalHttpServerMetrics);
    return this;
  }

  /** Returns a new {@link NettyServerTelemetry} with the given configuration. */
  public NettyServerTelemetry build() {
    return new NettyServerTelemetry(
        builder.build(),
        emitExperimentalHttpServerEvents
            ? ProtocolEventHandler.Enabled.INSTANCE
            : ProtocolEventHandler.Noop.INSTANCE);
  }
}
