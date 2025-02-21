/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.aerospike;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.ResultCode;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.javaagent.instrumentation.aerospike.internal.AerospikeRequest;
import io.opentelemetry.javaagent.instrumentation.aerospike.internal.AerospikeSemanticAttributes;
import javax.annotation.Nullable;

final class AerospikeClientAttributeExtractor
    implements AttributesExtractor<AerospikeRequest, Void> {

  @Override
  public void onStart(
      AttributesBuilder attributes, Context parentContext, AerospikeRequest aerospikeRequest) {
    attributes.put(AerospikeSemanticAttributes.AEROSPIKE_SET_NAME, aerospikeRequest.getSet());
  }

  @Override
  public void onEnd(
      AttributesBuilder attributes,
      Context context,
      AerospikeRequest aerospikeRequest,
      @Nullable Void unused,
      @Nullable Throwable error) {
    if (aerospikeRequest.getNode() != null) {
      String nodeName = aerospikeRequest.getNode().getName();
      attributes.put(AerospikeSemanticAttributes.AEROSPIKE_NODE_NAME, nodeName);
    }

    if (error != null) {
      if (error instanceof AerospikeException) {
        AerospikeException aerospikeException = (AerospikeException) error;
        attributes.put(
            AerospikeSemanticAttributes.AEROSPIKE_STATUS, aerospikeException.getResultCode());
      } else {
        attributes.put(AerospikeSemanticAttributes.AEROSPIKE_STATUS, ResultCode.CLIENT_ERROR);
      }
    } else {
      attributes.put(AerospikeSemanticAttributes.AEROSPIKE_STATUS, ResultCode.OK);
    }
  }
}
