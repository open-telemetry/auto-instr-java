/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.netty.common.v4_0.internal;

import io.netty.channel.Channel;
import io.netty.channel.socket.DatagramChannel;
import javax.annotation.Nullable;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class ChannelUtil {

  public static String getNetworkTransport(@Nullable Channel channel) {
    if (channel == null) {
      return null;
    }
    return channel instanceof DatagramChannel ? "udp" : "tcp";
  }

  private ChannelUtil() {}
}
