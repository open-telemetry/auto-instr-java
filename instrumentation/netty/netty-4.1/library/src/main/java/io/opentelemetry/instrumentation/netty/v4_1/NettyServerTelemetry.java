/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.netty.v4_1;

import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.CombinedChannelDuplexHandler;
import io.netty.handler.codec.http.HttpResponse;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.netty.common.v4_0.HttpRequestAndChannel;
import io.opentelemetry.instrumentation.netty.v4_1.internal.ProtocolEventHandler;
import io.opentelemetry.instrumentation.netty.v4_1.internal.server.HttpServerRequestTracingHandler;
import io.opentelemetry.instrumentation.netty.v4_1.internal.server.HttpServerResponseBeforeCommitHandler;
import io.opentelemetry.instrumentation.netty.v4_1.internal.server.HttpServerResponseTracingHandler;
import io.opentelemetry.instrumentation.netty.v4_1.internal.server.HttpServerTracingHandler;

/** Entrypoint for instrumenting Netty HTTP servers. */
public final class NettyServerTelemetry {

  private final Instrumenter<HttpRequestAndChannel, HttpResponse> instrumenter;
  private final ProtocolEventHandler protocolEventHandler;

  NettyServerTelemetry(
      Instrumenter<HttpRequestAndChannel, HttpResponse> instrumenter,
      ProtocolEventHandler protocolEventHandler) {
    this.instrumenter = instrumenter;
    this.protocolEventHandler = protocolEventHandler;
  }

  /** Returns a new {@link NettyServerTelemetry} configured with the given {@link OpenTelemetry}. */
  public static NettyServerTelemetry create(OpenTelemetry openTelemetry) {
    return builder(openTelemetry).build();
  }

  /**
   * Returns a new {@link NettyServerTelemetryBuilder} configured with the given {@link
   * OpenTelemetry}.
   */
  public static NettyServerTelemetryBuilder builder(OpenTelemetry openTelemetry) {
    return new NettyServerTelemetryBuilder(openTelemetry);
  }

  /**
   * Returns a new {@link ChannelInboundHandlerAdapter} that generates telemetry for incoming HTTP
   * requests. Must be paired with {@link #createResponseHandler()}.
   */
  public ChannelInboundHandlerAdapter createRequestHandler() {
    return new HttpServerRequestTracingHandler(instrumenter);
  }

  /**
   * Returns a new {@link ChannelOutboundHandlerAdapter} that generates telemetry for outgoing HTTP
   * responses. Must be paired with {@link #createRequestHandler()}.
   */
  public ChannelOutboundHandlerAdapter createResponseHandler() {
    return createResponseHandler(HttpServerResponseBeforeCommitHandler.Noop.INSTANCE);
  }

  public ChannelOutboundHandlerAdapter createResponseHandler(
      HttpServerResponseBeforeCommitHandler commitHandler) {
    return new HttpServerResponseTracingHandler(instrumenter, commitHandler, protocolEventHandler);
  }

  /**
   * Returns a new {@link CombinedChannelDuplexHandler} that generates telemetry for incoming HTTP
   * requests and outgoing responses in a single handler.
   */
  public CombinedChannelDuplexHandler<
          ? extends ChannelInboundHandlerAdapter, ? extends ChannelOutboundHandlerAdapter>
      createCombinedHandler() {
    return createCombinedHandler(HttpServerResponseBeforeCommitHandler.Noop.INSTANCE);
  }

  public CombinedChannelDuplexHandler<
          ? extends ChannelInboundHandlerAdapter, ? extends ChannelOutboundHandlerAdapter>
      createCombinedHandler(HttpServerResponseBeforeCommitHandler commitHandler) {
    return new HttpServerTracingHandler(instrumenter, commitHandler, protocolEventHandler);
  }
}
