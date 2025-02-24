/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.netty.v4_0.client;

import static io.opentelemetry.javaagent.instrumentation.netty.v4_0.client.HttpClientRequestTracingHandler.HTTP_CLIENT_REQUEST;
import static io.opentelemetry.javaagent.instrumentation.netty.v4_0.client.NettyClientSingletons.instrumenter;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.netty.common.v4_0.HttpRequestAndChannel;
import io.opentelemetry.javaagent.instrumentation.netty.v4_0.AttributeKeys;

public class HttpClientResponseTracingHandler extends ChannelInboundHandlerAdapter {

  private static final AttributeKey<HttpResponse> HTTP_CLIENT_RESPONSE =
      AttributeKeys.attributeKey(AttributeKeys.class.getName() + ".http-client-response");

  @Override
  public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
    Attribute<Context> contextAttr = ctx.channel().attr(AttributeKeys.CLIENT_CONTEXT);
    Context context = contextAttr.get();
    if (context == null) {
      super.channelRead(ctx, msg);
      return;
    }

    Attribute<Context> parentContextAttr = ctx.channel().attr(AttributeKeys.CLIENT_PARENT_CONTEXT);
    Context parentContext = parentContextAttr.get();

    if (msg instanceof FullHttpResponse) {
      HttpRequestAndChannel request = ctx.channel().attr(HTTP_CLIENT_REQUEST).getAndSet(null);
      instrumenter().end(context, request, (HttpResponse) msg, null);
      contextAttr.remove();
      parentContextAttr.remove();
    } else if (msg instanceof HttpResponse) {
      // Headers before body have been received, store them to use when finishing the span.
      ctx.channel().attr(HTTP_CLIENT_RESPONSE).set((HttpResponse) msg);
    } else if (msg instanceof LastHttpContent) {
      // Not a FullHttpResponse so this is content that has been received after headers.
      // Finish the span using what we stored in attrs.
      HttpRequestAndChannel request = ctx.channel().attr(HTTP_CLIENT_REQUEST).getAndSet(null);
      HttpResponse response = ctx.channel().attr(HTTP_CLIENT_RESPONSE).getAndRemove();
      instrumenter().end(context, request, response, null);
      contextAttr.remove();
      parentContextAttr.remove();
    }

    // We want the callback in the scope of the parent, not the client span
    if (parentContext != null) {
      try (Scope ignored = parentContext.makeCurrent()) {
        super.channelRead(ctx, msg);
      }
    } else {
      super.channelRead(ctx, msg);
    }
  }
}
