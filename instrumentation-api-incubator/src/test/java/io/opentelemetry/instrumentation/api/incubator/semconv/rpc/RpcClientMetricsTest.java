/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.incubator.semconv.rpc;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.OperationListener;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import io.opentelemetry.semconv.NetworkAttributes;
import io.opentelemetry.semconv.ServerAttributes;
import io.opentelemetry.semconv.incubating.RpcIncubatingAttributes;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class RpcClientMetricsTest {

  @Test
  void collectsMetrics() {
    InMemoryMetricReader metricReader = InMemoryMetricReader.createDelta();
    SdkMeterProvider meterProvider =
        SdkMeterProvider.builder().registerMetricReader(metricReader).build();

    OperationListener listener = RpcClientMetrics.get().create(meterProvider.get("test"));

    Attributes requestAttributes =
        Attributes.builder()
            .put(RpcIncubatingAttributes.RPC_SYSTEM, "grpc")
            .put(RpcIncubatingAttributes.RPC_SERVICE, "myservice.EchoService")
            .put(RpcIncubatingAttributes.RPC_METHOD, "exampleMethod")
            .build();

    Attributes responseAttributes1 =
        Attributes.builder()
            .put(ServerAttributes.SERVER_ADDRESS, "example.com")
            .put(ServerAttributes.SERVER_PORT, 8080)
            .put(NetworkAttributes.NETWORK_TRANSPORT, "tcp")
            .put(NetworkAttributes.NETWORK_TYPE, "ipv4")
            .put(RpcCommonAttributesExtractor.RPC_REQUEST_BODY_SIZE, 10)
            .put(RpcCommonAttributesExtractor.RPC_RESPONSE_BODY_SIZE, 20)
            .build();

    Attributes responseAttributes2 =
        Attributes.builder()
            .put(ServerAttributes.SERVER_PORT, 8080)
            .put(NetworkAttributes.NETWORK_TRANSPORT, "tcp")
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

    Context context1 = listener.onStart(parent, requestAttributes, nanos(100));

    assertThat(metricReader.collectAllMetrics()).isEmpty();

    Context context2 = listener.onStart(Context.root(), requestAttributes, nanos(150));

    assertThat(metricReader.collectAllMetrics()).isEmpty();

    listener.onEnd(context1, responseAttributes1, nanos(250));

    assertThat(metricReader.collectAllMetrics())
        .satisfiesExactlyInAnyOrder(
            metric ->
                assertThat(metric)
                    .hasName("rpc.client.response.size")
                    .hasUnit("By")
                    .hasDescription("Measures the size of RPC response messages (uncompressed).")
                    .hasHistogramSatisfying(
                        histogram ->
                            histogram.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasSum(20 /* bytes */)
                                        .hasAttributesSatisfying(
                                            equalTo(RpcIncubatingAttributes.RPC_SYSTEM, "grpc"),
                                            equalTo(
                                                RpcIncubatingAttributes.RPC_SERVICE,
                                                "myservice.EchoService"),
                                            equalTo(
                                                RpcIncubatingAttributes.RPC_METHOD,
                                                "exampleMethod"),
                                            equalTo(ServerAttributes.SERVER_ADDRESS, "example.com"),
                                            equalTo(ServerAttributes.SERVER_PORT, 8080),
                                            equalTo(NetworkAttributes.NETWORK_TRANSPORT, "tcp"),
                                            equalTo(NetworkAttributes.NETWORK_TYPE, "ipv4"))
                                        .hasExemplarsSatisfying(
                                            exemplar ->
                                                exemplar
                                                    .hasTraceId("ff01020304050600ff0a0b0c0d0e0f00")
                                                    .hasSpanId("090a0b0c0d0e0f00")))),
            metric ->
                assertThat(metric)
                    .hasName("rpc.client.request.size")
                    .hasUnit("By")
                    .hasDescription("Measures the size of RPC request messages (uncompressed).")
                    .hasHistogramSatisfying(
                        histogram ->
                            histogram.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasSum(10 /* bytes */)
                                        .hasAttributesSatisfying(
                                            equalTo(RpcIncubatingAttributes.RPC_SYSTEM, "grpc"),
                                            equalTo(
                                                RpcIncubatingAttributes.RPC_SERVICE,
                                                "myservice.EchoService"),
                                            equalTo(
                                                RpcIncubatingAttributes.RPC_METHOD,
                                                "exampleMethod"),
                                            equalTo(ServerAttributes.SERVER_ADDRESS, "example.com"),
                                            equalTo(ServerAttributes.SERVER_PORT, 8080),
                                            equalTo(NetworkAttributes.NETWORK_TRANSPORT, "tcp"),
                                            equalTo(NetworkAttributes.NETWORK_TYPE, "ipv4"))
                                        .hasExemplarsSatisfying(
                                            exemplar ->
                                                exemplar
                                                    .hasTraceId("ff01020304050600ff0a0b0c0d0e0f00")
                                                    .hasSpanId("090a0b0c0d0e0f00")))),
            metric ->
                assertThat(metric)
                    .hasName("rpc.client.duration")
                    .hasUnit("ms")
                    .hasHistogramSatisfying(
                        histogram ->
                            histogram.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasSum(150 /* millis */)
                                        .hasAttributesSatisfying(
                                            equalTo(RpcIncubatingAttributes.RPC_SYSTEM, "grpc"),
                                            equalTo(
                                                RpcIncubatingAttributes.RPC_SERVICE,
                                                "myservice.EchoService"),
                                            equalTo(
                                                RpcIncubatingAttributes.RPC_METHOD,
                                                "exampleMethod"),
                                            equalTo(ServerAttributes.SERVER_ADDRESS, "example.com"),
                                            equalTo(ServerAttributes.SERVER_PORT, 8080),
                                            equalTo(NetworkAttributes.NETWORK_TRANSPORT, "tcp"),
                                            equalTo(NetworkAttributes.NETWORK_TYPE, "ipv4"))
                                        .hasExemplarsSatisfying(
                                            exemplar ->
                                                exemplar
                                                    .hasTraceId("ff01020304050600ff0a0b0c0d0e0f00")
                                                    .hasSpanId("090a0b0c0d0e0f00")))));

    listener.onEnd(context2, responseAttributes2, nanos(300));

    assertThat(metricReader.collectAllMetrics())
        .satisfiesExactlyInAnyOrder(
            metric ->
                assertThat(metric)
                    .hasName("rpc.client.duration")
                    .hasUnit("ms")
                    .hasHistogramSatisfying(
                        histogram ->
                            histogram.hasPointsSatisfying(
                                point ->
                                    point
                                        .hasSum(150 /* millis */)
                                        .hasAttributesSatisfying(
                                            equalTo(RpcIncubatingAttributes.RPC_SYSTEM, "grpc"),
                                            equalTo(
                                                RpcIncubatingAttributes.RPC_SERVICE,
                                                "myservice.EchoService"),
                                            equalTo(
                                                RpcIncubatingAttributes.RPC_METHOD,
                                                "exampleMethod"),
                                            equalTo(ServerAttributes.SERVER_PORT, 8080),
                                            equalTo(NetworkAttributes.NETWORK_TRANSPORT, "tcp")))));
  }

  private static long nanos(int millis) {
    return TimeUnit.MILLISECONDS.toNanos(millis);
  }
}
