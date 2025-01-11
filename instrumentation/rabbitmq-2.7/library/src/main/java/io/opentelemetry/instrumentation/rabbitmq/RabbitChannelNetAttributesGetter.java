/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.rabbitmq;

import io.opentelemetry.instrumentation.api.semconv.network.NetworkAttributesGetter;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import javax.annotation.Nullable;

public class RabbitChannelNetAttributesGetter
    implements NetworkAttributesGetter<InstrumentedChannel, Void> {

  @Nullable
  @Override
  public String getNetworkType(InstrumentedChannel channelAndMethod, @Nullable Void unused) {
    InetAddress address = channelAndMethod.getChannel().getConnection().getAddress();
    if (address instanceof Inet4Address) {
      return "ipv4";
    } else if (address instanceof Inet6Address) {
      return "ipv6";
    }
    return null;
  }

  @Nullable
  @Override
  public String getNetworkPeerAddress(InstrumentedChannel channel, @Nullable Void unused) {
    return channel.getChannel().getConnection().getAddress().getHostAddress();
  }

  @Override
  public Integer getNetworkPeerPort(InstrumentedChannel channel, @Nullable Void unused) {
    return channel.getChannel().getConnection().getPort();
  }
}
