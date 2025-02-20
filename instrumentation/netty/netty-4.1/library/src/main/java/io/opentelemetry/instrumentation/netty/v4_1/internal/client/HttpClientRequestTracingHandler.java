/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.netty.v4_1.internal.client;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOutboundHandlerAdapter;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.util.Attribute;
import io.netty.util.AttributeKey;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.netty.common.v4_0.HttpRequestAndChannel;
import io.opentelemetry.instrumentation.netty.v4_1.internal.AttributeKeys;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public class HttpClientRequestTracingHandler extends ChannelOutboundHandlerAdapter {

  public static final AttributeKey<HttpRequestAndChannel> HTTP_CLIENT_REQUEST =
      AttributeKey.valueOf(HttpClientRequestTracingHandler.class, "http-client-request");

  private final Instrumenter<HttpRequestAndChannel, HttpResponse> instrumenter;

  public HttpClientRequestTracingHandler(
      Instrumenter<HttpRequestAndChannel, HttpResponse> instrumenter) {
    this.instrumenter = instrumenter;
  }

  @Override
  public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise prm) throws Exception {
    if (!(msg instanceof HttpRequest)) {
      super.write(ctx, msg, prm);
      return;
    }

    Context parentContext = ctx.channel().attr(AttributeKeys.CLIENT_PARENT_CONTEXT).get();
    if (parentContext == null) {
      parentContext = Context.current();
    }

    HttpRequestAndChannel request = HttpRequestAndChannel.create((HttpRequest) msg, ctx.channel());
    if (!instrumenter.shouldStart(parentContext, request) || isAwsRequest(request)) {
      super.write(ctx, msg, prm);
      return;
    }

    Attribute<Context> parentContextAttr = ctx.channel().attr(AttributeKeys.CLIENT_PARENT_CONTEXT);
    Attribute<Context> contextAttr = ctx.channel().attr(AttributeKeys.CLIENT_CONTEXT);
    Attribute<HttpRequestAndChannel> requestAttr = ctx.channel().attr(HTTP_CLIENT_REQUEST);

    Context context = instrumenter.start(parentContext, request);
    parentContextAttr.set(parentContext);
    contextAttr.set(context);
    requestAttr.set(request);

    try (Scope ignored = context.makeCurrent()) {
      super.write(ctx, msg, prm);
    } catch (Throwable throwable) {
      instrumenter.end(contextAttr.getAndSet(null), requestAttr.getAndSet(null), null, throwable);
      parentContextAttr.set(null);
      throw throwable;
    }
    // span is ended normally in HttpClientResponseTracingHandler
  }

  private static boolean isAwsRequest(HttpRequestAndChannel request) {
    // The AWS SDK uses Netty for asynchronous clients but constructs a request signature before
    // beginning transport. This means we MUST suppress Netty spans we would normally create or
    // they will inject their own trace header, which does not match what was present when the
    // signature was computed, breaking the SDK request completely. We have not found how to
    // cleanly propagate context from the SDK instrumentation, which executes on an application
    // thread, to Netty instrumentation, which executes on event loops. If it's possible, it may
    // require instrumenting internal classes. Using a header which is more or less guaranteed to
    // always exist is arguably more stable.
    return request.request().headers().contains("amz-sdk-invocation-id");
  }
}
