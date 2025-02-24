/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.netty.common.v4_0.internal.client;

import io.netty.channel.Channel;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.netty.common.internal.NettyConnectionRequest;
import javax.annotation.Nullable;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public interface NettyConnectionInstrumenter {

  boolean shouldStart(Context parentContext, NettyConnectionRequest request);

  Context start(Context parentContext, NettyConnectionRequest request);

  void end(
      Context context, NettyConnectionRequest request, Channel channel, @Nullable Throwable error);
}
