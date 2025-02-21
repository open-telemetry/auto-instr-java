/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.aerospike.internal;

import com.aerospike.client.cluster.Node;
import com.google.auto.value.AutoValue;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
@AutoValue
public abstract class AerospikeRequest {
  private Node node;

  public static AerospikeRequest create(String operation, String namespace, String set) {
    return new AutoValue_AerospikeRequest(operation, namespace, set);
  }

  public abstract String getOperation();

  public abstract String getNamespace();

  public abstract String getSet();

  public void setNode(Node node) {
    this.node = node;
  }

  public Node getNode() {
    return this.node;
  }
}
