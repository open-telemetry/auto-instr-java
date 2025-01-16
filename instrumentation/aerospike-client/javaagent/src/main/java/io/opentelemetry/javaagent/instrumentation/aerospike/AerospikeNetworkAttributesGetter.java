/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.aerospike;

import com.aerospike.client.cluster.Node;
import io.opentelemetry.instrumentation.api.semconv.network.NetworkAttributesGetter;
import io.opentelemetry.javaagent.instrumentation.aerospike.internal.AerospikeRequest;
import java.net.InetSocketAddress;
import javax.annotation.Nullable;

final class AerospikeNetworkAttributesGetter
    implements NetworkAttributesGetter<AerospikeRequest, Void> {

  @Override
  @Nullable
  public InetSocketAddress getNetworkPeerInetSocketAddress(
      AerospikeRequest aerospikeRequest, @Nullable Void unused) {
    Node node = aerospikeRequest.getNode();
    if (node != null) {
      return node.getAddress();
    }
    return null;
  }
}
