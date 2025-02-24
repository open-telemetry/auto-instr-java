/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.netty.v4.common;

import io.netty.channel.ChannelPromise;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.netty.common.internal.NettyConnectionRequest;
import io.opentelemetry.instrumentation.netty.common.v4_0.internal.client.ConnectionCompleteListener;
import io.opentelemetry.instrumentation.netty.common.v4_0.internal.client.NettyConnectionInstrumenter;

/** Container used to carry state between enter and exit advices */
public class NettyScope {

  private final Context context;
  private final NettyConnectionRequest request;
  private final Scope scope;

  private NettyScope(Context context, NettyConnectionRequest request, Scope scope) {
    this.context = context;
    this.request = request;
    this.scope = scope;
  }

  public static NettyScope startNew(
      NettyConnectionInstrumenter instrumenter,
      Context parentContext,
      NettyConnectionRequest request) {
    Context context = instrumenter.start(parentContext, request);
    return new NettyScope(context, request, context.makeCurrent());
  }

  public static void end(
      NettyScope nettyScope,
      NettyConnectionInstrumenter instrumenter,
      ChannelPromise channelPromise,
      Throwable throwable) {

    if (nettyScope == null) {
      return;
    }

    nettyScope.scope.close();

    if (throwable != null) {
      instrumenter.end(nettyScope.context, nettyScope.request, null, throwable);
    } else {
      channelPromise.addListener(
          new ConnectionCompleteListener(instrumenter, nettyScope.context, nettyScope.request));
    }
  }
}
