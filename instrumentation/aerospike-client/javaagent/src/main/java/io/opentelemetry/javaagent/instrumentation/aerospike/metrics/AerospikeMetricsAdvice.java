/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.aerospike.metrics;

import static io.opentelemetry.javaagent.instrumentation.aerospike.internal.AerospikeSemanticAttributes.AEROSPIKE_NODE_NAME;
import static io.opentelemetry.javaagent.instrumentation.aerospike.internal.AerospikeSemanticAttributes.AEROSPIKE_SET_NAME;
import static io.opentelemetry.javaagent.instrumentation.aerospike.internal.AerospikeSemanticAttributes.AEROSPIKE_STATUS;
import static io.opentelemetry.semconv.NetworkAttributes.NETWORK_PEER_ADDRESS;
import static io.opentelemetry.semconv.NetworkAttributes.NETWORK_PEER_PORT;
import static io.opentelemetry.semconv.NetworkAttributes.NETWORK_TYPE;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_NAME;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_OPERATION;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_SYSTEM;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.incubator.metrics.ExtendedDoubleHistogramBuilder;
import io.opentelemetry.api.incubator.metrics.ExtendedLongUpDownCounterBuilder;
import io.opentelemetry.api.metrics.DoubleHistogramBuilder;
import io.opentelemetry.api.metrics.LongUpDownCounterBuilder;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("deprecation") // using deprecated semconv
final class AerospikeMetricsAdvice {
  private AerospikeMetricsAdvice() {}

  static void applyConcurrencyUpDownCounterAdvice(LongUpDownCounterBuilder builder) {
    if (!(builder instanceof ExtendedLongUpDownCounterBuilder)) {
      return;
    }

    List<AttributeKey<?>> attributes = new ArrayList<>();
    attributes.add(DB_SYSTEM);
    attributes.add(DB_OPERATION);
    attributes.add(DB_NAME);
    attributes.add(AEROSPIKE_SET_NAME);

    ((ExtendedLongUpDownCounterBuilder) builder).setAttributesAdvice(attributes);
  }

  static void applyClientDurationAdvice(DoubleHistogramBuilder builder) {
    if (!(builder instanceof ExtendedDoubleHistogramBuilder)) {
      return;
    }

    List<AttributeKey<?>> attributes = new ArrayList<>();
    attributes.add(DB_SYSTEM);
    attributes.add(DB_OPERATION);
    attributes.add(DB_NAME);
    attributes.add(AEROSPIKE_SET_NAME);
    attributes.add(AEROSPIKE_STATUS);
    attributes.add(NETWORK_PEER_ADDRESS);
    attributes.add(NETWORK_PEER_PORT);
    attributes.add(NETWORK_TYPE);
    attributes.add(AEROSPIKE_NODE_NAME);

    ((ExtendedDoubleHistogramBuilder) builder).setAttributesAdvice(attributes);
  }
}
