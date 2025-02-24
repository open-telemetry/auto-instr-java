/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.jsonrpc4j.v1_3;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.opentelemetry.semconv.incubating.RpcIncubatingAttributes.RPC_JSONRPC_VERSION;
import static io.opentelemetry.semconv.incubating.RpcIncubatingAttributes.RPC_METHOD;
import static io.opentelemetry.semconv.incubating.RpcIncubatingAttributes.RPC_SERVICE;
import static io.opentelemetry.semconv.incubating.RpcIncubatingAttributes.RPC_SYSTEM;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.googlecode.jsonrpc4j.JsonRpcBasicServer;
import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import com.googlecode.jsonrpc4j.ProxyUtil;
import com.googlecode.jsonrpc4j.spring.rest.JsonRpcRestClient;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.jsonrpc4j.v1_3.AbstractJsonRpcTest;
import io.opentelemetry.instrumentation.jsonrpc4j.v1_3.CalculatorService;
import io.opentelemetry.instrumentation.jsonrpc4j.v1_3.CalculatorServiceImpl;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class AgentJsonRpcTest extends AbstractJsonRpcTest {

  @RegisterExtension
  static final InstrumentationExtension testing = AgentInstrumentationExtension.create();

  @Override
  protected InstrumentationExtension testing() {
    return testing;
  }

  @Override
  protected JsonRpcBasicServer configureServer(JsonRpcBasicServer server) {
    return server;
  }

  @Test
  void testClient() throws Throwable {
    CalculatorService clientProxy =
        ProxyUtil.createClientProxy(
            this.getClass().getClassLoader(), CalculatorService.class, getHttpClient());
    int res =
        testing()
            .runWithSpan(
                "parent",
                () -> {
                  return clientProxy.add(1, 2);
                });

    assertThat(res).isEqualTo(3);

    testing()
        .waitAndAssertTraces(
            trace ->
                trace.hasSpansSatisfyingExactly(
                    span -> span.hasName("parent").hasKind(SpanKind.INTERNAL).hasNoParent(),
                    span ->
                        span.hasName(
                                "io.opentelemetry.instrumentation.jsonrpc4j.v1_3.CalculatorService/add")
                            .hasKind(SpanKind.CLIENT)
                            .hasParent(trace.getSpan(0))
                            .hasAttributesSatisfyingExactly(
                                equalTo(RPC_SYSTEM, "jsonrpc"),
                                equalTo(RPC_JSONRPC_VERSION, "2.0"),
                                equalTo(
                                    RPC_SERVICE,
                                    "io.opentelemetry.instrumentation.jsonrpc4j.v1_3.CalculatorService"),
                                equalTo(RPC_METHOD, "add"))),
            trace -> trace.hasSpansSatisfyingExactly(span -> span.hasKind(SpanKind.SERVER)));

    testing()
        .waitAndAssertMetrics(
            "io.opentelemetry.jsonrpc4j-1.3",
            "rpc.client.duration",
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

  private JettyServer jettyServer;

  @BeforeAll
  public void setup() throws Exception {
    this.jettyServer = createServer();
  }

  private static JettyServer createServer() throws Exception {
    JettyServer jettyServer = new JettyServer(CalculatorServiceImpl.class);
    jettyServer.startup();
    return jettyServer;
  }

  protected JsonRpcRestClient getClient() throws MalformedURLException {
    return getClient(JettyServer.SERVLET);
  }

  protected JsonRpcRestClient getClient(String servlet) throws MalformedURLException {
    return new JsonRpcRestClient(new URL(jettyServer.getCustomServerUrlString(servlet)));
  }

  protected JsonRpcHttpClient getHttpClient() throws MalformedURLException {
    Map<String, String> header = new HashMap<>();
    return new JsonRpcHttpClient(
        new ObjectMapper(),
        new URL(jettyServer.getCustomServerUrlString(JettyServer.SERVLET)),
        header);
  }

  protected JsonRpcHttpClient getHttpClient(String servlet) throws MalformedURLException {
    Map<String, String> header = new HashMap<>();
    return new JsonRpcHttpClient(
        new ObjectMapper(), new URL(jettyServer.getCustomServerUrlString(servlet)), header);
  }

  @AfterAll
  public void teardown() throws Exception {
    jettyServer.stop();
  }
}
