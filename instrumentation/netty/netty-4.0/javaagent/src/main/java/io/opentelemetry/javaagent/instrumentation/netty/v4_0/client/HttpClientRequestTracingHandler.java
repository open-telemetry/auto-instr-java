/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.netty.v4_0.client;

import static io.opentelemetry.javaagent.instrumentation.netty.v4_0.client.NettyClientSingletons.instrumenter;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.netty.common.v4_0.HttpRequestAndChannel;
import io.opentelemetry.javaagent.instrumentation.netty.v4_0.AttributeKeys;

public class HttpClientRequestTracingHandler extends ChannelOutboundHandlerAdapter {

  public static final AttributeKey<HttpRequestAndChannel> HTTP_CLIENT_REQUEST =
      AttributeKeys.attributeKey(AttributeKeys.class.getName() + ".http-client-request");

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise prm) throws Exception {
    if (!(msg instanceof HttpRequest)) {
      super.write(ctx, msg, prm);
      return;
    }

    Context parentContext = Context.current();

    HttpRequestAndChannel request = HttpRequestAndChannel.create((HttpRequest) msg, ctx.channel());
    if (!instrumenter().shouldStart(parentContext, request)) {
      super.write(ctx, msg, prm);
      return;
    }

    Attribute<Context> parentContextAttr = ctx.channel().attr(AttributeKeys.CLIENT_PARENT_CONTEXT);
    Attribute<Context> contextAttr = ctx.channel().attr(AttributeKeys.CLIENT_CONTEXT);
    Attribute<HttpRequestAndChannel> requestAttr = ctx.channel().attr(HTTP_CLIENT_REQUEST);

    Context context = instrumenter().start(parentContext, request);
    parentContextAttr.set(parentContext);
    contextAttr.set(context);
    requestAttr.set(request);

    try (Scope ignored = context.makeCurrent()) {
      super.write(ctx, msg, prm);
    } catch (Throwable throwable) {
      instrumenter().end(contextAttr.getAndRemove(), requestAttr.getAndRemove(), null, throwable);
      parentContextAttr.remove();
      throw throwable;
    }
    // span is ended normally in HttpClientResponseTracingHandler
  }
}
