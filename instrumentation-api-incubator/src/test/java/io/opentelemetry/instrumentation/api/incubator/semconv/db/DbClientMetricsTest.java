/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.incubator.semconv.db;

import static io.opentelemetry.instrumentation.api.internal.SemconvStability.emitStableDatabaseSemconv;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.OperationListener;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import io.opentelemetry.semconv.ErrorAttributes;
import io.opentelemetry.semconv.NetworkAttributes;
import io.opentelemetry.semconv.ServerAttributes;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class DbClientMetricsTest {

  static final double[] DURATION_BUCKETS =
      DbClientMetricsAdvice.DURATION_SECONDS_BUCKETS.stream().mapToDouble(d -> d).toArray();

  @Test
  void collectsMetrics() {
    assumeTrue(emitStableDatabaseSemconv());

    InMemoryMetricReader metricReader = InMemoryMetricReader.create();
    SdkMeterProvider meterProvider =
        SdkMeterProvider.builder().registerMetricReader(metricReader).build();

    OperationListener listener = DbClientMetrics.get().create(meterProvider.get("test"));

    Attributes operationAttributes =
        Attributes.builder()
            .put(DbClientCommonAttributesExtractor.DB_SYSTEM_NAME, "myDb")
            .put(SqlClientAttributesExtractor.DB_COLLECTION_NAME, "table")
            .put(DbClientCommonAttributesExtractor.DB_NAMESPACE, "potatoes")
            .put(DbClientAttributesExtractor.DB_OPERATION_NAME, "SELECT")
            .put(ServerAttributes.SERVER_ADDRESS, "localhost")
            .put(ServerAttributes.SERVER_PORT, 1234)
            .build();

    Attributes responseAttributes =
        Attributes.builder()
            .put(DbClientAttributesExtractor.DB_RESPONSE_STATUS_CODE, "200")
            .put(ErrorAttributes.ERROR_TYPE, "400")
            .put(NetworkAttributes.NETWORK_PEER_ADDRESS, "1.2.3.4")
            .put(NetworkAttributes.NETWORK_PEER_PORT, 8080)
            .build();

    Context parent =
        Context.root()
            .with(
                Span.wrap(
                    SpanContext.create(
                        "ff01020304050600ff0a0b0c0d0e0f00",
                        "090a0b0c0d0e0f00",
                        TraceFlags.getSampled(),
                        TraceState.getDefault())));

    Context context1 = listener.onStart(parent, operationAttributes, nanos(100));

    assertThat(metricReader.collectAllMetrics()).isEmpty();

    listener.onEnd(context1, responseAttributes, nanos(250));

    assertThat(metricReader.collectAllMetrics())
        .satisfiesExactlyInAnyOrder(
            metric ->
                assertThat(metric)
                    .hasName("db.client.operation.duration")
                    .hasUnit("s")
                    .hasDescription("Duration of database client operations.")
                    .hasHistogramSatisfying(
                        histogram ->
                            histogram.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasSum(0.15 /* seconds */)
                                        .hasAttributesSatisfying(
                                            equalTo(
                                                DbClientCommonAttributesExtractor.DB_SYSTEM_NAME,
                                                "myDb"),
                                            equalTo(
                                                DbClientCommonAttributesExtractor.DB_NAMESPACE,
                                                "potatoes"),
                                            equalTo(
                                                DbClientAttributesExtractor.DB_OPERATION_NAME,
                                                "SELECT"),
                                            equalTo(
                                                SqlClientAttributesExtractor.DB_COLLECTION_NAME,
                                                "table"),
                                            equalTo(ServerAttributes.SERVER_ADDRESS, "localhost"),
                                            equalTo(ServerAttributes.SERVER_PORT, 1234),
                                            equalTo(
                                                DbClientAttributesExtractor.DB_RESPONSE_STATUS_CODE,
                                                "200"),
                                            equalTo(ErrorAttributes.ERROR_TYPE, "400"),
                                            equalTo(
                                                NetworkAttributes.NETWORK_PEER_ADDRESS, "1.2.3.4"),
                                            equalTo(NetworkAttributes.NETWORK_PEER_PORT, 8080))
                                        .hasExemplarsSatisfying(
                                            exemplar ->
                                                exemplar
                                                    .hasTraceId("ff01020304050600ff0a0b0c0d0e0f00")
                                                    .hasSpanId("090a0b0c0d0e0f00"))
                                        .hasBucketBoundaries(DURATION_BUCKETS))));
  }

  private static long nanos(int millis) {
    return TimeUnit.MILLISECONDS.toNanos(millis);
  }
}
