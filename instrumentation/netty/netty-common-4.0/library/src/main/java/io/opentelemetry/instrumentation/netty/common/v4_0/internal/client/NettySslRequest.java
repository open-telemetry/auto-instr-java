/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.netty.common.v4_0.internal.client;

import com.google.auto.value.AutoValue;
import io.netty.channel.Channel;
import java.net.SocketAddress;
import javax.annotation.Nullable;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
@AutoValue
public abstract class NettySslRequest {

  static NettySslRequest create(Channel channel) {
    return new AutoValue_NettySslRequest(channel, channel.remoteAddress());
  }

  String spanName() {
    return "SSL handshake";
  }

  abstract Channel channel();

  @Nullable
  abstract SocketAddress remoteAddress();
}
