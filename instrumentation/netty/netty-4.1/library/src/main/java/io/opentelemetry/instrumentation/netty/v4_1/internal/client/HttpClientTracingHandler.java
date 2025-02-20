/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.netty.v4_1.internal.client;

import static io.opentelemetry.instrumentation.netty.v4_1.internal.client.HttpClientRequestTracingHandler.HTTP_CLIENT_REQUEST;
import static io.opentelemetry.instrumentation.netty.v4_1.internal.client.HttpClientResponseTracingHandler.HTTP_CLIENT_RESPONSE;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.channel.CombinedChannelDuplexHandler;
import io.netty.handler.codec.http.HttpResponse;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.netty.common.v4_0.HttpRequestAndChannel;
import io.opentelemetry.instrumentation.netty.v4_1.internal.AttributeKeys;
import io.opentelemetry.instrumentation.netty.v4_1.internal.ProtocolEventHandler;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public class HttpClientTracingHandler
    extends CombinedChannelDuplexHandler<
        HttpClientResponseTracingHandler, HttpClientRequestTracingHandler> {
  private final Instrumenter<HttpRequestAndChannel, HttpResponse> instrumenter;

  public HttpClientTracingHandler(
      Instrumenter<HttpRequestAndChannel, HttpResponse> instrumenter,
      ProtocolEventHandler protocolEventHandler) {
    super(
        new HttpClientResponseTracingHandler(instrumenter, protocolEventHandler),
        new HttpClientRequestTracingHandler(instrumenter));
    this.instrumenter = instrumenter;
  }

  @Override
  public void close(ChannelHandlerContext ctx, ChannelPromise promise) throws Exception {
    Context context = ctx.channel().attr(AttributeKeys.CLIENT_CONTEXT).getAndSet(null);
    HttpRequestAndChannel request = ctx.channel().attr(HTTP_CLIENT_REQUEST).getAndSet(null);
    HttpResponse response = ctx.channel().attr(HTTP_CLIENT_RESPONSE).getAndSet(null);
    Context parentContext = ctx.channel().attr(AttributeKeys.CLIENT_PARENT_CONTEXT).getAndSet(null);
    if (context != null && request != null) {
      instrumenter.end(context, request, response, null);
    }

    if (parentContext != null) {
      try (Scope ignored = parentContext.makeCurrent()) {
        super.close(ctx, promise);
      }
    } else {
      super.close(ctx, promise);
    }
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    // javaagent inserts exception handling in AbstractChannelHandlerContextInstrumentation that
    // runs before this code
    Context context = ctx.channel().attr(AttributeKeys.CLIENT_CONTEXT).getAndSet(null);
    HttpRequestAndChannel request = ctx.channel().attr(HTTP_CLIENT_REQUEST).getAndSet(null);
    HttpResponse response = ctx.channel().attr(HTTP_CLIENT_RESPONSE).getAndSet(null);
    Context parentContext = ctx.channel().attr(AttributeKeys.CLIENT_PARENT_CONTEXT).getAndSet(null);
    if (context != null && request != null) {
      instrumenter.end(context, request, response, cause);
    }

    if (parentContext != null) {
      try (Scope ignored = parentContext.makeCurrent()) {
        super.exceptionCaught(ctx, cause);
      }
    } else {
      super.exceptionCaught(ctx, cause);
    }
  }
}
