/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.aerospike;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.api.incubator.config.internal.InstrumentationConfig;
import io.opentelemetry.instrumentation.api.incubator.semconv.db.DbClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.db.DbClientSpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor;
import io.opentelemetry.instrumentation.api.semconv.network.NetworkAttributesExtractor;
import io.opentelemetry.instrumentation.api.semconv.network.NetworkAttributesGetter;
import io.opentelemetry.javaagent.bootstrap.internal.AgentInstrumentationConfig;
import io.opentelemetry.javaagent.instrumentation.aerospike.internal.AerospikeRequest;
import io.opentelemetry.javaagent.instrumentation.aerospike.metrics.AerospikeMetrics;

public final class AerospikeSingletons {
  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.aerospike-client";

  private static final Instrumenter<AerospikeRequest, Void> INSTRUMENTER;

  static {
    AerospikeDbAttributesGetter aerospikeDbAttributesGetter = new AerospikeDbAttributesGetter();
    NetworkAttributesGetter<AerospikeRequest, Void> netAttributesGetter =
        new AerospikeNetworkAttributesGetter();

    InstrumenterBuilder<AerospikeRequest, Void> builder =
        Instrumenter.<AerospikeRequest, Void>builder(
                GlobalOpenTelemetry.get(),
                INSTRUMENTATION_NAME,
                DbClientSpanNameExtractor.create(aerospikeDbAttributesGetter))
            .addAttributesExtractor(DbClientAttributesExtractor.create(aerospikeDbAttributesGetter))
            .addAttributesExtractor(NetworkAttributesExtractor.create(netAttributesGetter));
    InstrumentationConfig instrumentationConfig = AgentInstrumentationConfig.get();
    if (instrumentationConfig.getBoolean(
        "otel.instrumentation.aerospike.experimental-span-attributes", false)) {
      builder.addAttributesExtractor(new AerospikeClientAttributeExtractor());
    }
    if (instrumentationConfig.getBoolean(
        "otel.instrumentation.aerospike.experimental-metrics", false)) {
      builder.addOperationMetrics(AerospikeMetrics.get());
    }

    INSTRUMENTER = builder.buildInstrumenter(SpanKindExtractor.alwaysClient());
  }

  public static Instrumenter<AerospikeRequest, Void> instrumenter() {
    return INSTRUMENTER;
  }

  private AerospikeSingletons() {}
}
