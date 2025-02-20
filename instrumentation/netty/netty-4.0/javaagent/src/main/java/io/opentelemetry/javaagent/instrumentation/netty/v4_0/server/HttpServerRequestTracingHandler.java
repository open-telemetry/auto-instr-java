/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.netty.v4_0.server;

import static io.opentelemetry.javaagent.instrumentation.netty.v4_0.server.NettyServerSingletons.instrumenter;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.netty.common.v4_0.HttpRequestAndChannel;
import io.opentelemetry.javaagent.instrumentation.netty.v4_0.AttributeKeys;

public class HttpServerRequestTracingHandler extends ChannelInboundHandlerAdapter {

  static final AttributeKey<HttpRequestAndChannel> HTTP_SERVER_REQUEST =
      AttributeKeys.attributeKey(AttributeKeys.class.getName() + ".http-server-request");

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    Channel channel = ctx.channel();
    Attribute<Context> contextAttr = channel.attr(AttributeKeys.SERVER_CONTEXT);
    Attribute<HttpRequestAndChannel> requestAttr = channel.attr(HTTP_SERVER_REQUEST);

    if (!(msg instanceof HttpRequest)) {
      Context serverContext = contextAttr.get();
      if (serverContext == null) {
        super.channelRead(ctx, msg);
      } else {
        try (Scope ignored = serverContext.makeCurrent()) {
          super.channelRead(ctx, msg);
        }
      }
      return;
    }

    Context parentContext = contextAttr.get();
    if (parentContext == null) {
      parentContext = Context.current();
    }
    HttpRequestAndChannel request = HttpRequestAndChannel.create((HttpRequest) msg, channel);

    if (!instrumenter().shouldStart(parentContext, request)) {
      super.channelRead(ctx, msg);
      return;
    }

    Context context = instrumenter().start(parentContext, request);
    contextAttr.set(context);
    requestAttr.set(request);

    try (Scope ignored = context.makeCurrent()) {
      super.channelRead(ctx, msg);
    } catch (Throwable throwable) {
      // make sure to remove the server context on end() call
      instrumenter().end(contextAttr.getAndRemove(), requestAttr.getAndRemove(), null, throwable);
      throw throwable;
    }
    // the span is ended normally in HttpServerResponseTracingHandler
  }
}
