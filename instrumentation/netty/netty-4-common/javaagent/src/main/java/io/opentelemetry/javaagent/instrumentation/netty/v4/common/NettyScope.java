/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.netty.v4.common;

import io.netty.channel.ChannelPromise;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.netty.common.internal.NettyConnectionRequest;
import io.opentelemetry.instrumentation.netty.v4.common.internal.client.ConnectionCompleteListener;
import io.opentelemetry.instrumentation.netty.v4.common.internal.client.NettyConnectionInstrumenter;

/** Container used to carry state between enter and exit advices */
public class NettyScope {

  Context context;
  NettyConnectionRequest request;
  Scope scope;

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
      Object scope,
      NettyConnectionInstrumenter instrumenter,
      ChannelPromise channelPromise,
      Throwable throwable) {
    if (!(scope instanceof NettyScope)) {
      return;
    }
    NettyScope nettyScope = (NettyScope) scope;

    nettyScope.scope.close();

    if (throwable != null) {
      instrumenter.end(nettyScope.context, nettyScope.request, null, throwable);
    } else {
      channelPromise.addListener(
          new ConnectionCompleteListener(instrumenter, nettyScope.context, nettyScope.request));
    }
  }
}
