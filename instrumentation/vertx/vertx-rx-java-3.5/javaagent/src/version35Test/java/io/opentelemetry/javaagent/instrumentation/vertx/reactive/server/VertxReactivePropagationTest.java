/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.vertx.reactive.server;

import static io.opentelemetry.api.common.AttributeKey.longKey;
import static io.opentelemetry.instrumentation.api.internal.SemconvStability.emitStableDatabaseSemconv;
import static io.opentelemetry.instrumentation.testing.junit.db.SemconvStabilityUtil.maybeStable;
import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.SUCCESS;
import static io.opentelemetry.javaagent.instrumentation.vertx.reactive.server.VertxReactiveWebServer.TEST_REQUEST_ID_ATTRIBUTE;
import static io.opentelemetry.javaagent.instrumentation.vertx.reactive.server.VertxReactiveWebServer.TEST_REQUEST_ID_PARAMETER;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.satisfies;
import static io.opentelemetry.semconv.ClientAttributes.CLIENT_ADDRESS;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_REQUEST_METHOD;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_RESPONSE_STATUS_CODE;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_ROUTE;
import static io.opentelemetry.semconv.NetworkAttributes.NETWORK_PEER_ADDRESS;
import static io.opentelemetry.semconv.NetworkAttributes.NETWORK_PEER_PORT;
import static io.opentelemetry.semconv.NetworkAttributes.NETWORK_PROTOCOL_VERSION;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_ADDRESS;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_PORT;
import static io.opentelemetry.semconv.UrlAttributes.URL_PATH;
import static io.opentelemetry.semconv.UrlAttributes.URL_QUERY;
import static io.opentelemetry.semconv.UrlAttributes.URL_SCHEME;
import static io.opentelemetry.semconv.UserAgentAttributes.USER_AGENT_ORIGINAL;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_CONNECTION_STRING;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_NAME;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_OPERATION;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_SQL_TABLE;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_STATEMENT;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_SYSTEM;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_USER;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.instrumentation.test.utils.PortUtils;
import io.opentelemetry.instrumentation.testing.internal.AutoCleanupExtension;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.sdk.testing.assertj.TraceAssert;
import io.opentelemetry.testing.internal.armeria.client.WebClient;
import io.opentelemetry.testing.internal.armeria.common.AggregatedHttpResponse;
import io.opentelemetry.testing.internal.armeria.common.HttpRequest;
import io.opentelemetry.testing.internal.armeria.common.HttpRequestBuilder;
import io.vertx.reactivex.core.Vertx;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class VertxReactivePropagationTest {
  @RegisterExtension
  static final InstrumentationExtension testing = AgentInstrumentationExtension.create();

  @RegisterExtension static final AutoCleanupExtension cleanup = AutoCleanupExtension.create();

  private static WebClient client;
  private static int port;
  private static Vertx server;

  @BeforeAll
  static void setUp() throws ExecutionException, InterruptedException, TimeoutException {
    port = PortUtils.findOpenPort();
    server = VertxReactiveWebServer.start(port);
    client = WebClient.of("h1c://localhost:" + port);
  }

  @AfterAll
  static void cleanUp() {
    server.close();
  }

  // Verifies that context is correctly propagated and sql query span has correct parent.
  // Tests io.opentelemetry.javaagent.instrumentation.vertx.reactive.VertxRxInstrumentation
  @SuppressWarnings("deprecation") // uses deprecated db semconv
  @Test
  void contextPropagation() {
    AggregatedHttpResponse response = client.get("/listProducts").aggregate().join();
    assertThat(response.status().code()).isEqualTo(SUCCESS.getStatus());

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("GET /listProducts")
                        .hasKind(SpanKind.SERVER)
                        .hasNoParent()
                        .hasAttributesSatisfyingExactly(
                            equalTo(NETWORK_PROTOCOL_VERSION, "1.1"),
                            equalTo(NETWORK_PEER_ADDRESS, "127.0.0.1"),
                            satisfies(NETWORK_PEER_PORT, val -> val.isInstanceOf(Long.class)),
                            equalTo(SERVER_ADDRESS, "localhost"),
                            equalTo(SERVER_PORT, port),
                            equalTo(CLIENT_ADDRESS, "127.0.0.1"),
                            equalTo(URL_PATH, "/listProducts"),
                            equalTo(HTTP_REQUEST_METHOD, "GET"),
                            equalTo(HTTP_RESPONSE_STATUS_CODE, 200),
                            equalTo(URL_SCHEME, "http"),
                            satisfies(USER_AGENT_ORIGINAL, val -> val.isInstanceOf(String.class)),
                            equalTo(HTTP_ROUTE, "/listProducts")),
                span ->
                    span.hasName("handleListProducts")
                        .hasKind(SpanKind.INTERNAL)
                        .hasParent(trace.getSpan(0)),
                span ->
                    span.hasName("listProducts")
                        .hasKind(SpanKind.INTERNAL)
                        .hasParent(trace.getSpan(1)),
                span ->
                    span.hasName("SELECT test.products")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(2))
                        .hasAttributesSatisfyingExactly(
                            equalTo(DB_SYSTEM, "hsqldb"),
                            equalTo(maybeStable(DB_NAME), "test"),
                            equalTo(DB_USER, emitStableDatabaseSemconv() ? null : "SA"),
                            equalTo(
                                DB_CONNECTION_STRING,
                                emitStableDatabaseSemconv() ? null : "hsqldb:mem:"),
                            equalTo(
                                maybeStable(DB_STATEMENT),
                                "SELECT id, name, price, weight FROM products"),
                            equalTo(maybeStable(DB_OPERATION), "SELECT"),
                            equalTo(maybeStable(DB_SQL_TABLE), "products"))));
  }

  @SuppressWarnings("deprecation") // uses deprecated db semconv
  @Test
  void highConcurrency() {
    int count = 100;
    String baseUrl = "/listProducts";
    CountDownLatch latch = new CountDownLatch(1);

    ExecutorService pool = Executors.newFixedThreadPool(8);
    cleanup.deferCleanup(pool::shutdownNow);
    TextMapPropagator propagator = GlobalOpenTelemetry.getPropagators().getTextMapPropagator();
    TextMapSetter<HttpRequestBuilder> setter =
        (carrier, name, value) -> carrier.header(name, value);

    for (int i = 0; i < count; i++) {
      int index = i;
      pool.submit(
          () -> {
            try {
              latch.await();
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            }
            testing.runWithSpan(
                "client " + index,
                () -> {
                  HttpRequestBuilder builder =
                      HttpRequest.builder()
                          .get(baseUrl + "?" + TEST_REQUEST_ID_PARAMETER + "=" + index);
                  Span.current().setAttribute(TEST_REQUEST_ID_ATTRIBUTE, index);
                  propagator.inject(Context.current(), builder, setter);
                  client.execute(builder.build()).aggregate().join();
                });
          });
    }

    latch.countDown();

    List<Consumer<TraceAssert>> assertions = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      assertions.add(
          trace -> {
            long requestId =
                Long.parseLong(trace.getSpan(0).getName().substring("client ".length()));
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("client " + requestId)
                        .hasKind(SpanKind.INTERNAL)
                        .hasNoParent()
                        .hasAttributesSatisfyingExactly(
                            equalTo(longKey(TEST_REQUEST_ID_ATTRIBUTE), requestId)),
                span ->
                    span.hasName("GET /listProducts")
                        .hasKind(SpanKind.SERVER)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(NETWORK_PROTOCOL_VERSION, "1.1"),
                            equalTo(NETWORK_PEER_ADDRESS, "127.0.0.1"),
                            satisfies(NETWORK_PEER_PORT, val -> val.isInstanceOf(Long.class)),
                            equalTo(SERVER_ADDRESS, "localhost"),
                            equalTo(SERVER_PORT, port),
                            equalTo(CLIENT_ADDRESS, "127.0.0.1"),
                            equalTo(URL_PATH, baseUrl),
                            equalTo(URL_QUERY, TEST_REQUEST_ID_PARAMETER + "=" + requestId),
                            equalTo(HTTP_REQUEST_METHOD, "GET"),
                            equalTo(HTTP_RESPONSE_STATUS_CODE, 200),
                            equalTo(URL_SCHEME, "http"),
                            satisfies(USER_AGENT_ORIGINAL, val -> val.isInstanceOf(String.class)),
                            equalTo(HTTP_ROUTE, "/listProducts"),
                            equalTo(longKey(TEST_REQUEST_ID_ATTRIBUTE), requestId)),
                span ->
                    span.hasName("handleListProducts")
                        .hasKind(SpanKind.INTERNAL)
                        .hasParent(trace.getSpan(1))
                        .hasAttributesSatisfyingExactly(
                            equalTo(longKey(TEST_REQUEST_ID_ATTRIBUTE), requestId)),
                span ->
                    span.hasName("listProducts")
                        .hasKind(SpanKind.INTERNAL)
                        .hasParent(trace.getSpan(2))
                        .hasAttributesSatisfyingExactly(
                            equalTo(longKey(TEST_REQUEST_ID_ATTRIBUTE), requestId)),
                span ->
                    span.hasName("SELECT test.products")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(3))
                        .hasAttributesSatisfyingExactly(
                            equalTo(DB_SYSTEM, "hsqldb"),
                            equalTo(maybeStable(DB_NAME), "test"),
                            equalTo(DB_USER, emitStableDatabaseSemconv() ? null : "SA"),
                            equalTo(
                                DB_CONNECTION_STRING,
                                emitStableDatabaseSemconv() ? null : "hsqldb:mem:"),
                            equalTo(
                                maybeStable(DB_STATEMENT),
                                "SELECT id AS request"
                                    + requestId
                                    + ", name, price, weight FROM products"),
                            equalTo(maybeStable(DB_OPERATION), "SELECT"),
                            equalTo(maybeStable(DB_SQL_TABLE), "products")));
          });
    }
    testing.waitAndAssertTraces(assertions);
  }
}
