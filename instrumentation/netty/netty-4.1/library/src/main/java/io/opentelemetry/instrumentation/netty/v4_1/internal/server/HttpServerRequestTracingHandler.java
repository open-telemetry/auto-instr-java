/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.netty.v4_1.internal.server;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.netty.common.v4_0.HttpRequestAndChannel;
import io.opentelemetry.instrumentation.netty.v4_1.internal.ServerContext;
import io.opentelemetry.instrumentation.netty.v4_1.internal.ServerContexts;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public class HttpServerRequestTracingHandler extends ChannelInboundHandlerAdapter {

  private final Instrumenter<HttpRequestAndChannel, HttpResponse> instrumenter;

  public HttpServerRequestTracingHandler(
      Instrumenter<HttpRequestAndChannel, HttpResponse> instrumenter) {
    this.instrumenter = instrumenter;
  }

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    Channel channel = ctx.channel();
    ServerContexts serverContexts = ServerContexts.getOrCreate(channel);

    if (!(msg instanceof HttpRequest)) {
      ServerContext serverContext = serverContexts.peekLast();
      if (serverContext == null) {
        super.channelRead(ctx, msg);
      } else {
        try (Scope ignored = serverContext.context().makeCurrent()) {
          super.channelRead(ctx, msg);
        }
      }
      return;
    }

    Context parentContext = Context.current();
    HttpRequestAndChannel request = HttpRequestAndChannel.create((HttpRequest) msg, channel);
    if (!instrumenter.shouldStart(parentContext, request)) {
      super.channelRead(ctx, msg);
      return;
    }

    Context context = instrumenter.start(parentContext, request);
    serverContexts.addLast(ServerContext.create(context, request));

    try (Scope ignored = context.makeCurrent()) {
      super.channelRead(ctx, msg);
    } catch (Throwable t) {
      // make sure to remove the server context on end() call
      ServerContext serverContext = serverContexts.pollLast();
      if (serverContext != null) {
        instrumenter.end(serverContext.context(), serverContext.request(), null, t);
      }
      throw t;
    }
    // span is ended normally in HttpServerResponseTracingHandler
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    // connection was closed, close all remaining requests
    ServerContexts serverContexts = ServerContexts.get(ctx.channel());

    if (serverContexts == null) {
      super.channelInactive(ctx);
      return;
    }

    ServerContext serverContext;
    while ((serverContext = serverContexts.pollFirst()) != null) {
      instrumenter.end(serverContext.context(), serverContext.request(), null, null);
    }
    super.channelInactive(ctx);
  }
}
