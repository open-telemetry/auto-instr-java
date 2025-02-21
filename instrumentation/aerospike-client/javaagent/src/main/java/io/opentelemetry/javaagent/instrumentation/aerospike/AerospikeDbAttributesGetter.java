/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.aerospike;

import io.opentelemetry.instrumentation.api.incubator.semconv.db.DbClientAttributesGetter;
import io.opentelemetry.javaagent.instrumentation.aerospike.internal.AerospikeRequest;
import io.opentelemetry.javaagent.instrumentation.aerospike.internal.AerospikeSemanticAttributes;
import javax.annotation.Nullable;

final class AerospikeDbAttributesGetter implements DbClientAttributesGetter<AerospikeRequest> {

  @Override
  public String getDbSystem(AerospikeRequest request) {
    return AerospikeSemanticAttributes.DbSystemValues.AEROSPIKE;
  }

  @Override
  public String getDbOperationName(AerospikeRequest request) {
    return request.getOperation();
  }

  @Nullable
  @Override
  public String getDbNamespace(AerospikeRequest request) {
    return request.getNamespace();
  }

  @Deprecated
  @Nullable
  @Override
  public String getUser(AerospikeRequest aerospikeRequest) {
    return null;
  }

  @Deprecated
  @Nullable
  @Override
  public String getConnectionString(AerospikeRequest aerospikeRequest) {
    return null;
  }
}
