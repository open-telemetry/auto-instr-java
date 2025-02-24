/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.netty.common.v4_0.internal.client;

import io.opentelemetry.instrumentation.api.semconv.network.NetworkAttributesGetter;
import io.opentelemetry.instrumentation.netty.common.v4_0.internal.ChannelUtil;
import java.net.InetSocketAddress;
import javax.annotation.Nullable;

final class NettySslNetAttributesGetter implements NetworkAttributesGetter<NettySslRequest, Void> {

  @Override
  public String getNetworkTransport(NettySslRequest request, @Nullable Void unused) {
    return ChannelUtil.getNetworkTransport(request.channel());
  }

  @Nullable
  @Override
  public InetSocketAddress getNetworkPeerInetSocketAddress(
      NettySslRequest request, @Nullable Void unused) {
    if (request.remoteAddress() instanceof InetSocketAddress) {
      return (InetSocketAddress) request.remoteAddress();
    }
    return null;
  }
}
