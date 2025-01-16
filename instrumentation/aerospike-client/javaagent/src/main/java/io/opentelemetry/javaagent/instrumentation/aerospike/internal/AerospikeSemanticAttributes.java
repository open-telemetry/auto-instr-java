/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.aerospike.internal;

import static io.opentelemetry.api.common.AttributeKey.longKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;

import io.opentelemetry.api.common.AttributeKey;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class AerospikeSemanticAttributes {
  private AerospikeSemanticAttributes() {}

  public static final AttributeKey<Long> AEROSPIKE_STATUS = longKey("db.status");
  public static final AttributeKey<String> AEROSPIKE_SET_NAME = stringKey("db.set.name");
  public static final AttributeKey<String> AEROSPIKE_NODE_NAME = stringKey("db.node.name");

  /**
   * This class is internal and is hence not for public use. Its APIs are unstable and can change at
   * any time.
   */
  public static final class DbSystemValues {
    public static final String AEROSPIKE = "aerospike";

    private DbSystemValues() {}
  }
}
