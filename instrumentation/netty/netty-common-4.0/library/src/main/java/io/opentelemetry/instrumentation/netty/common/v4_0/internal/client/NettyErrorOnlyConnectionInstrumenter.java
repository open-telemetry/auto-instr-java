/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.netty.common.v4_0.internal.client;

import io.netty.channel.Channel;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.internal.InstrumenterUtil;
import io.opentelemetry.instrumentation.netty.common.internal.NettyConnectionRequest;
import io.opentelemetry.instrumentation.netty.common.internal.Timer;
import javax.annotation.Nullable;

final class NettyErrorOnlyConnectionInstrumenter implements NettyConnectionInstrumenter {

  private final Instrumenter<NettyConnectionRequest, Channel> instrumenter;

  NettyErrorOnlyConnectionInstrumenter(Instrumenter<NettyConnectionRequest, Channel> instrumenter) {
    this.instrumenter = instrumenter;
  }

  @Override
  public boolean shouldStart(Context parentContext, NettyConnectionRequest request) {
    // the "real" check is done on end() anyway
    return true;
  }

  @Override
  public Context start(Context parentContext, NettyConnectionRequest request) {
    return parentContext.with(Timer.start());
  }

  @Override
  public void end(
      Context context, NettyConnectionRequest request, Channel channel, @Nullable Throwable error) {
    if (error != null && instrumenter.shouldStart(context, request)) {
      Timer timer = Timer.get(context);
      InstrumenterUtil.startAndEnd(
          instrumenter, context, request, channel, error, timer.startTime(), timer.now());
    }
  }
}
