/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.jsonrpc4j.v1_3;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.opentelemetry.semconv.incubating.RpcIncubatingAttributes.RPC_JSONRPC_VERSION;
import static io.opentelemetry.semconv.incubating.RpcIncubatingAttributes.RPC_METHOD;
import static io.opentelemetry.semconv.incubating.RpcIncubatingAttributes.RPC_SERVICE;
import static io.opentelemetry.semconv.incubating.RpcIncubatingAttributes.RPC_SYSTEM;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.googlecode.jsonrpc4j.JsonRpcBasicServer;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.testing.internal.jackson.databind.JsonNode;
import io.opentelemetry.testing.internal.jackson.databind.ObjectMapper;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractJsonRpcTest {

  protected abstract InstrumentationExtension testing();

  protected abstract JsonRpcBasicServer configureServer(JsonRpcBasicServer server);

  @Test
  void testServer() throws IOException {
    CalculatorService calculator = new CalculatorServiceImpl();
    JsonRpcBasicServer server =
        configureServer(new JsonRpcBasicServer(calculator, CalculatorService.class));

    JsonNode response =
        testing()
            .runWithSpan(
                "parent",
                () -> {
                  InputStream inputStream =
                      new ByteArrayInputStream(
                          "{\"jsonrpc\":\"2.0\",\"method\":\"add\",\"params\":[1,2],\"id\":1}"
                              .getBytes(UTF_8));
                  ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                  server.handleRequest(inputStream, outputStream);

                  // Read the JsonNode from the InputStream
                  ObjectMapper objectMapper = new ObjectMapper();
                  return objectMapper.readTree(
                      new ByteArrayInputStream(outputStream.toByteArray()));
                });

    assertThat(response.get("result").asInt()).isEqualTo(3);

    testing()
        .waitAndAssertTraces(
            trace ->
                trace.hasSpansSatisfyingExactly(
                    span -> span.hasName("parent").hasKind(SpanKind.INTERNAL).hasNoParent(),
                    span ->
                        span.hasName(
                                "io.opentelemetry.instrumentation.jsonrpc4j.v1_3.CalculatorService/add")
                            .hasKind(SpanKind.SERVER)
                            .hasParent(trace.getSpan(0))
                            .hasAttributesSatisfyingExactly(
                                equalTo(RPC_SYSTEM, "jsonrpc"),
                                equalTo(RPC_JSONRPC_VERSION, "2.0"),
                                equalTo(
                                    RPC_SERVICE,
                                    "io.opentelemetry.instrumentation.jsonrpc4j.v1_3.CalculatorService"),
                                equalTo(RPC_METHOD, "add"))));
    testing()
        .waitAndAssertMetrics(
            "io.opentelemetry.jsonrpc4j-1.3",
            "rpc.server.duration",
            metrics ->
                metrics.anySatisfy(
                    metric ->
                        assertThat(metric)
                            .hasUnit("ms")
                            .hasHistogramSatisfying(
                                histogram ->
                                    histogram.hasPointsSatisfying(
                                        point ->
                                            point.hasAttributesSatisfying(
                                                equalTo(RPC_METHOD, "add"),
                                                equalTo(
                                                    RPC_SERVICE,
                                                    "io.opentelemetry.instrumentation.jsonrpc4j.v1_3.CalculatorService"),
                                                equalTo(RPC_SYSTEM, "jsonrpc"))))));
  }
}
