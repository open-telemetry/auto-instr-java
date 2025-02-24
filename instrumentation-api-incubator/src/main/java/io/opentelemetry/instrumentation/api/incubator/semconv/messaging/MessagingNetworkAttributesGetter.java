/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.incubator.semconv.messaging;

import io.opentelemetry.instrumentation.api.semconv.network.ServerAttributesGetter;
import io.opentelemetry.instrumentation.api.semconv.network.internal.InetSocketAddressUtil;
import java.net.InetSocketAddress;
import javax.annotation.Nullable;

public interface MessagingNetworkAttributesGetter<REQUEST, RESPONSE>
    extends ServerAttributesGetter<REQUEST> {

  @Nullable
  default InetSocketAddress getNetworkPeerInetSocketAddress(
      REQUEST request, @Nullable RESPONSE response) {
    return null;
  }

  @Nullable
  default String getNetworkPeerAddress(REQUEST request, @Nullable RESPONSE response) {
    return InetSocketAddressUtil.getIpAddress(getNetworkPeerInetSocketAddress(request, response));
  }

  @Nullable
  default Integer getNetworkPeerPort(REQUEST request, @Nullable RESPONSE response) {
    return InetSocketAddressUtil.getPort(getNetworkPeerInetSocketAddress(request, response));
  }
}
