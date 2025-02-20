/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.netty.v4_1;

import io.netty.channel.Channel;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.CombinedChannelDuplexHandler;
import io.netty.handler.codec.http.HttpResponse;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.netty.common.v4_0.HttpRequestAndChannel;
import io.opentelemetry.instrumentation.netty.v4_1.internal.AttributeKeys;
import io.opentelemetry.instrumentation.netty.v4_1.internal.client.NettyClientHandlerFactory;

/** Entrypoint for instrumenting Netty HTTP clients. */
public final class NettyClientTelemetry {

  private final NettyClientHandlerFactory handlerFactory;

  NettyClientTelemetry(
      Instrumenter<HttpRequestAndChannel, HttpResponse> instrumenter,
      boolean emitExperimentalHttpClientEvents) {
    this.handlerFactory =
        new NettyClientHandlerFactory(instrumenter, emitExperimentalHttpClientEvents);
  }

  /** Returns a new {@link NettyClientTelemetry} configured with the given {@link OpenTelemetry}. */
  public static NettyClientTelemetry create(OpenTelemetry openTelemetry) {
    return builder(openTelemetry).build();
  }

  /**
   * Returns a new {@link NettyClientTelemetryBuilder} configured with the given {@link
   * OpenTelemetry}.
   */
  public static NettyClientTelemetryBuilder builder(OpenTelemetry openTelemetry) {
    return new NettyClientTelemetryBuilder(openTelemetry);
  }

  /**
   * Returns a new {@link ChannelOutboundHandlerAdapter} that generates telemetry for outgoing HTTP
   * requests. Must be paired with {@link #createResponseHandler()}.
   */
  public ChannelOutboundHandlerAdapter createRequestHandler() {
    return handlerFactory.createRequestHandler();
  }

  /**
   * Returns a new {@link ChannelInboundHandlerAdapter} that generates telemetry for incoming HTTP
   * responses. Must be paired with {@link #createRequestHandler()}.
   */
  public ChannelInboundHandlerAdapter createResponseHandler() {
    return handlerFactory.createResponseHandler();
  }

  /**
   * Returns a new {@link CombinedChannelDuplexHandler} that generates telemetry for outgoing HTTP
   * requests and incoming responses in a single handler.
   */
  public CombinedChannelDuplexHandler<
          ? extends ChannelInboundHandlerAdapter, ? extends ChannelOutboundHandlerAdapter>
      createCombinedHandler() {
    return handlerFactory.createCombinedHandler();
  }

  /**
   * Propagate the {@link Context} to the {@link Channel}. This MUST be called before each HTTP
   * request executed on a {@link Channel}.
   */
  // TODO (trask) rename to setParentContext()?
  public static void setChannelContext(Channel channel, Context context) {
    channel.attr(AttributeKeys.CLIENT_PARENT_CONTEXT).set(context);
  }
}
