/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.netty.common.v4_0.internal.client;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.netty.channel.Channel;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.netty.common.internal.NettyConnectionRequest;
import javax.annotation.Nullable;

enum NoopConnectionInstrumenter implements NettyConnectionInstrumenter {
  INSTANCE;

  @Override
  public boolean shouldStart(Context parentContext, NettyConnectionRequest request) {
    return false;
  }

  @CanIgnoreReturnValue
  @Override
  public Context start(Context parentContext, NettyConnectionRequest request) {
    return parentContext;
  }

  @Override
  public void end(
      Context context,
      NettyConnectionRequest request,
      Channel channel,
      @Nullable Throwable error) {}
}
