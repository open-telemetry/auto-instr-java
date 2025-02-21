/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.rabbitmq;

import com.rabbitmq.client.GetResponse;
import io.opentelemetry.instrumentation.api.semconv.network.NetworkAttributesGetter;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import javax.annotation.Nullable;

public class RabbitReceiveNetAttributesGetter
    implements NetworkAttributesGetter<ReceiveRequest, GetResponse> {

  @Nullable
  @Override
  public String getNetworkType(ReceiveRequest request, @Nullable GetResponse response) {
    InetAddress address = request.getConnection().getAddress();
    if (address instanceof Inet4Address) {
      return "ipv6";
    } else if (address instanceof Inet6Address) {
      return "ipv6";
    }
    return null;
  }

  @Nullable
  @Override
  public String getNetworkPeerAddress(ReceiveRequest request, @Nullable GetResponse response) {
    return request.getConnection().getAddress().getHostAddress();
  }

  @Nullable
  @Override
  public Integer getNetworkPeerPort(ReceiveRequest request, @Nullable GetResponse response) {
    return request.getConnection().getPort();
  }
}
