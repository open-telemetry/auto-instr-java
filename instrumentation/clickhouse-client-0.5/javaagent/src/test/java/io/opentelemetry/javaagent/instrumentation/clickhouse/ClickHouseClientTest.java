/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.clickhouse;

import static io.opentelemetry.instrumentation.testing.junit.db.SemconvStabilityUtil.maybeStable;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.opentelemetry.semconv.ErrorAttributes.ERROR_TYPE;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_ADDRESS;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_PORT;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_NAME;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_OPERATION;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_RESPONSE_STATUS_CODE;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_STATEMENT;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_SYSTEM;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

import com.clickhouse.client.ClickHouseClient;
import com.clickhouse.client.ClickHouseException;
import com.clickhouse.client.ClickHouseNode;
import com.clickhouse.client.ClickHouseParameterizedQuery;
import com.clickhouse.client.ClickHouseRequest;
import com.clickhouse.client.ClickHouseResponse;
import com.clickhouse.client.ClickHouseResponseSummary;
import com.clickhouse.data.ClickHouseFormat;
import com.google.common.collect.ImmutableMap;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.sdk.testing.assertj.AttributeAssertion;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.semconv.incubating.DbIncubatingAttributes;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.GenericContainer;

@TestInstance(Lifecycle.PER_CLASS)
class ClickHouseClientTest {

  @RegisterExtension
  private static final InstrumentationExtension testing = AgentInstrumentationExtension.create();

  private static final GenericContainer<?> clickhouseServer =
      new GenericContainer<>("clickhouse/clickhouse-server:24.4.2").withExposedPorts(8123);

  private static final String dbName = "default";
  private static final String tableName = "test_table";
  private static int port;
  private static String host;
  private static ClickHouseNode server;
  private static ClickHouseClient client;

  @BeforeAll
  void setup() throws ClickHouseException {
    clickhouseServer.start();
    port = clickhouseServer.getMappedPort(8123);
    host = clickhouseServer.getHost();
    server = ClickHouseNode.of("http://" + host + ":" + port + "/" + dbName + "?compress=0");
    client = ClickHouseClient.builder().build();

    ClickHouseResponse response =
        client
            .read(server)
            .query("create table if not exists " + tableName + "(s String) engine=Memory")
            .executeAndWait();
    response.close();

    // wait for CREATE operation and clear
    testing.waitForTraces(1);
    testing.clearData();
  }

  @AfterAll
  void cleanup() {
    if (client != null) {
      client.close();
    }
    clickhouseServer.stop();
  }

  @Test
  void testConnectionStringWithoutDatabaseSpecifiedStillGeneratesSpans()
      throws ClickHouseException {
    ClickHouseNode server = ClickHouseNode.of("http://" + host + ":" + port + "?compress=0");
    ClickHouseClient client = ClickHouseClient.builder().build();

    ClickHouseResponse response =
        client
            .read(server)
            .format(ClickHouseFormat.RowBinaryWithNamesAndTypes)
            .query("select * from " + tableName)
            .executeAndWait();
    response.close();

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("SELECT " + dbName)
                        .hasKind(SpanKind.CLIENT)
                        .hasNoParent()
                        .hasAttributesSatisfyingExactly(
                            attributeAssertions("select * from " + tableName, "SELECT"))));
  }

  @Test
  void testExecuteAndWaitWithStringQuery() throws ClickHouseException {
    testing.runWithSpan(
        "parent",
        () -> {
          ClickHouseResponse response;
          response =
              client
                  .write(server)
                  .query("insert into " + tableName + " values('1')('2')('3')")
                  .executeAndWait();
          response.close();

          response =
              client
                  .read(server)
                  .format(ClickHouseFormat.RowBinaryWithNamesAndTypes)
                  .query("select * from " + tableName)
                  .executeAndWait();
          response.close();
        });

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasNoParent().hasAttributes(Attributes.empty()),
                span ->
                    span.hasName("INSERT " + dbName)
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            attributeAssertions(
                                "insert into " + tableName + " values(?)(?)(?)", "INSERT")),
                span ->
                    span.hasName("SELECT " + dbName)
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            attributeAssertions("select * from " + tableName, "SELECT"))));
  }

  @Test
  void testExecuteAndWaitWithStringQueryAndId() throws ClickHouseException {
    testing.runWithSpan(
        "parent",
        () -> {
          ClickHouseResponse response =
              client
                  .read(server)
                  .format(ClickHouseFormat.RowBinaryWithNamesAndTypes)
                  .query("select * from " + tableName, "test_query_id")
                  .executeAndWait();
          response.close();
        });

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasNoParent().hasAttributes(Attributes.empty()),
                span ->
                    span.hasName("SELECT " + dbName)
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            attributeAssertions("select * from " + tableName, "SELECT"))));
  }

  @Test
  void testExecuteAndWaitThrowsException() {
    Throwable thrown =
        catchThrowable(
            () -> {
              ClickHouseResponse response =
                  client
                      .read(server)
                      .format(ClickHouseFormat.RowBinaryWithNamesAndTypes)
                      .query("select * from non_existent_table")
                      .executeAndWait();
              response.close();
            });

    assertThat(thrown).isInstanceOf(ClickHouseException.class);

    List<AttributeAssertion> assertions =
        new ArrayList<>(attributeAssertions("select * from non_existent_table", "SELECT"));
    assertions.add(equalTo(DB_RESPONSE_STATUS_CODE, "60"));
    assertions.add(equalTo(ERROR_TYPE, "com.clickhouse.client.ClickHouseException"));
    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("SELECT " + dbName)
                        .hasKind(SpanKind.CLIENT)
                        .hasStatus(StatusData.error())
                        .hasException(thrown)
                        .hasAttributesSatisfyingExactly(assertions)));
  }

  @Test
  void testAsyncExecuteQuery() throws Exception {
    CompletableFuture<ClickHouseResponse> response =
        client
            .read(server)
            .format(ClickHouseFormat.RowBinaryWithNamesAndTypes)
            .query("select * from " + tableName)
            .execute();

    ClickHouseResponse result = response.get();
    assertThat(result).isNotNull();
    result.close();

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("SELECT " + dbName)
                        .hasKind(SpanKind.CLIENT)
                        .hasAttributesSatisfyingExactly(
                            attributeAssertions("select * from " + tableName, "SELECT"))));
  }

  @Test
  void testSendQuery() throws Exception {
    testing.runWithSpan(
        "parent",
        () -> {
          CompletableFuture<List<ClickHouseResponseSummary>> future =
              ClickHouseClient.send(server, "select * from " + tableName + " limit 1");
          List<ClickHouseResponseSummary> results = future.get();
          assertThat(results).hasSize(1);
        });

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasNoParent().hasAttributes(Attributes.empty()),
                span ->
                    span.hasName("SELECT " + dbName)
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            attributeAssertions(
                                "select * from " + tableName + " limit ?", "SELECT"))));
  }

  @Test
  void testSendMultipleQueries() throws Exception {
    testing.runWithSpan(
        "parent",
        () -> {
          CompletableFuture<List<ClickHouseResponseSummary>> future =
              ClickHouseClient.send(
                  server,
                  "insert into " + tableName + " values('1')('2')('3')",
                  "select * from " + tableName + " limit 1");
          List<ClickHouseResponseSummary> results = future.get();
          assertThat(results).hasSize(2);
        });

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasNoParent().hasAttributes(Attributes.empty()),
                span ->
                    span.hasName("INSERT " + dbName)
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            attributeAssertions(
                                "insert into " + tableName + " values(?)(?)(?)", "INSERT")),
                span ->
                    span.hasName("SELECT " + dbName)
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            attributeAssertions(
                                "select * from " + tableName + " limit ?", "SELECT"))));
  }

  @Test
  void testParameterizedQueryInput() throws ClickHouseException {
    ClickHouseRequest<?> request =
        client.read(server).format(ClickHouseFormat.RowBinaryWithNamesAndTypes);

    testing.runWithSpan(
        "parent",
        () -> {
          ClickHouseResponse response =
              client
                  .write(server)
                  .query(
                      ClickHouseParameterizedQuery.of(
                          request.getConfig(),
                          "insert into " + tableName + " values(:val1)(:val2)(:val3)"))
                  .params(ImmutableMap.of("val1", "1", "val2", "2", "val3", "3"))
                  .executeAndWait();
          response.close();

          response =
              request
                  .query(
                      ClickHouseParameterizedQuery.of(
                          request.getConfig(), "select * from " + tableName + " where s=:val"))
                  .params(ImmutableMap.of("val", "'2'"))
                  .executeAndWait();
          response.close();
        });

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasNoParent().hasAttributes(Attributes.empty()),
                span ->
                    span.hasName("INSERT " + dbName)
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            attributeAssertions(
                                "insert into " + tableName + " values(:val1)(:val2)(:val3)",
                                "INSERT")),
                span ->
                    span.hasName("SELECT " + dbName)
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            attributeAssertions(
                                "select * from " + tableName + " where s=:val", "SELECT"))));
  }

  // regression test for
  // https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/13019
  // {s:String} used in the query really a syntax error, should be {s: String}. This test verifies
  // that this syntax error isn't detected when running with the agent as it is also ignored when
  // running without the agent.
  @Test
  void testPlaceholderQueryInput() throws Exception {
    ClickHouseRequest<?> request =
        client.read(server).format(ClickHouseFormat.RowBinaryWithNamesAndTypes);
    testing.runWithSpan(
        "parent",
        () -> {
          ClickHouseResponse response =
              request
                  // {s:String} is really a syntax error should be {s: String}
                  .query("select * from " + tableName + " where s={s:String}")
                  .settings(ImmutableMap.of("param_s", "" + Instant.now().getEpochSecond()))
                  .execute()
                  .get();
          response.close();
        });

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasNoParent().hasAttributes(Attributes.empty()),
                span ->
                    span.hasName("SELECT " + dbName)
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            attributeAssertions(
                                "select * from " + tableName + " where s={s:String}", "SELECT"))));
  }

  @SuppressWarnings("deprecation") // using deprecated semconv
  private static List<AttributeAssertion> attributeAssertions(String statement, String operation) {
    return asList(
        equalTo(maybeStable(DB_SYSTEM), DbIncubatingAttributes.DbSystemIncubatingValues.CLICKHOUSE),
        equalTo(maybeStable(DB_NAME), dbName),
        equalTo(SERVER_ADDRESS, host),
        equalTo(SERVER_PORT, port),
        equalTo(maybeStable(DB_STATEMENT), statement),
        equalTo(maybeStable(DB_OPERATION), operation));
  }
}
