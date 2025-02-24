/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.netty.common.v4_0.internal.client;

import io.netty.channel.Channel;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.netty.common.internal.NettyConnectionRequest;
import javax.annotation.Nullable;

final class NettyConnectionInstrumenterImpl implements NettyConnectionInstrumenter {

  private final Instrumenter<NettyConnectionRequest, Channel> instrumenter;

  NettyConnectionInstrumenterImpl(Instrumenter<NettyConnectionRequest, Channel> instrumenter) {
    this.instrumenter = instrumenter;
  }

  @Override
  public boolean shouldStart(Context parentContext, NettyConnectionRequest request) {
    return instrumenter.shouldStart(parentContext, request);
  }

  @Override
  public Context start(Context parentContext, NettyConnectionRequest request) {
    return instrumenter.start(parentContext, request);
  }

  @Override
  public void end(
      Context context, NettyConnectionRequest request, Channel channel, @Nullable Throwable error) {
    instrumenter.end(context, request, channel, error);
  }
}
