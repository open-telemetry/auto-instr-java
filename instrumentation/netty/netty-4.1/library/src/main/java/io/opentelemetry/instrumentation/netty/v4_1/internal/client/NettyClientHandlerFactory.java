/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.netty.v4_1.internal.client;

import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.CombinedChannelDuplexHandler;
import io.netty.handler.codec.http.HttpResponse;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.netty.common.v4_0.HttpRequestAndChannel;
import io.opentelemetry.instrumentation.netty.v4_1.internal.ProtocolEventHandler;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public class NettyClientHandlerFactory {

  private final Instrumenter<HttpRequestAndChannel, HttpResponse> instrumenter;
  private final ProtocolEventHandler protocolEventHandler;

  public NettyClientHandlerFactory(
      Instrumenter<HttpRequestAndChannel, HttpResponse> instrumenter,
      boolean emitExperimentalHttpClientEvents) {
    this.instrumenter = instrumenter;
    this.protocolEventHandler =
        emitExperimentalHttpClientEvents
            ? ProtocolEventHandler.Enabled.INSTANCE
            : ProtocolEventHandler.Noop.INSTANCE;
  }

  /**
   * Returns a new {@link ChannelOutboundHandlerAdapter} that generates telemetry for outgoing HTTP
   * requests. Must be paired with {@link #createResponseHandler()}.
   */
  public ChannelOutboundHandlerAdapter createRequestHandler() {
    return new HttpClientRequestTracingHandler(instrumenter);
  }

  /**
   * Returns a new {@link ChannelInboundHandlerAdapter} that generates telemetry for incoming HTTP
   * responses. Must be paired with {@link #createRequestHandler()}.
   */
  public ChannelInboundHandlerAdapter createResponseHandler() {
    return new HttpClientResponseTracingHandler(instrumenter, protocolEventHandler);
  }

  /**
   * Returns a new {@link CombinedChannelDuplexHandler} that generates telemetry for outgoing HTTP
   * requests and incoming responses in a single handler.
   */
  public CombinedChannelDuplexHandler<
          ? extends ChannelInboundHandlerAdapter, ? extends ChannelOutboundHandlerAdapter>
      createCombinedHandler() {
    return new HttpClientTracingHandler(instrumenter, protocolEventHandler);
  }
}
