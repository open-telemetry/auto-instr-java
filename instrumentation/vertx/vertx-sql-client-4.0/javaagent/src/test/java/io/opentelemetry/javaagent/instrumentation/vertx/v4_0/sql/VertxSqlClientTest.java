/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.vertx.v4_0.sql;

import static io.opentelemetry.instrumentation.api.internal.SemconvStability.emitStableDatabaseSemconv;
import static io.opentelemetry.instrumentation.testing.junit.db.SemconvStabilityUtil.maybeStable;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.satisfies;
import static io.opentelemetry.semconv.ErrorAttributes.ERROR_TYPE;
import static io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_MESSAGE;
import static io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_STACKTRACE;
import static io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_TYPE;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_ADDRESS;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_PORT;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_NAME;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_OPERATION;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_RESPONSE_STATUS_CODE;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_SQL_TABLE;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_STATEMENT;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_USER;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.testing.internal.AutoCleanupExtension;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.sdk.testing.assertj.TraceAssert;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgException;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Tuple;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;

@SuppressWarnings("deprecation") // using deprecated semconv
class VertxSqlClientTest {
  private static final Logger logger = LoggerFactory.getLogger(VertxSqlClientTest.class);

  private static final String USER_DB = "SA";
  private static final String PW_DB = "password123";
  private static final String DB = "tempdb";

  @RegisterExtension
  private static final InstrumentationExtension testing = AgentInstrumentationExtension.create();

  @RegisterExtension
  private static final AutoCleanupExtension cleanup = AutoCleanupExtension.create();

  private static GenericContainer<?> container;
  private static Vertx vertx;
  private static Pool pool;
  private static String host;
  private static int port;

  @BeforeAll
  static void setUp() throws Exception {
    container =
        new GenericContainer<>("postgres:9.6.8")
            .withEnv("POSTGRES_USER", USER_DB)
            .withEnv("POSTGRES_PASSWORD", PW_DB)
            .withEnv("POSTGRES_DB", DB)
            .withExposedPorts(5432)
            .withLogConsumer(new Slf4jLogConsumer(logger))
            .withStartupTimeout(Duration.ofMinutes(2));
    container.start();
    vertx = Vertx.vertx();
    host = container.getHost();
    port = container.getMappedPort(5432);
    PgConnectOptions options =
        new PgConnectOptions()
            .setPort(port)
            .setHost(host)
            .setDatabase(DB)
            .setUser(USER_DB)
            .setPassword(PW_DB);
    pool = Pool.pool(vertx, options, new PoolOptions().setMaxSize(4));
    pool.query("create table test(id int primary key, name varchar(255))")
        .execute()
        .compose(
            r ->
                // insert some test data
                pool.query("insert into test values (1, 'Hello'), (2, 'World')").execute())
        .toCompletionStage()
        .toCompletableFuture()
        .get(30, TimeUnit.SECONDS);
  }

  @AfterAll
  static void cleanUp() {
    pool.close();
    vertx.close();
    container.stop();
  }

  @Test
  void testSimpleSelect() throws Exception {
    CompletableFuture<Object> future = new CompletableFuture<>();
    CompletableFuture<Object> result =
        future.whenComplete((rows, throwable) -> testing.runWithSpan("callback", () -> {}));
    testing.runWithSpan(
        "parent",
        () ->
            pool.query("select * from test")
                .execute(
                    rowSetAsyncResult -> {
                      if (rowSetAsyncResult.succeeded()) {
                        future.complete(rowSetAsyncResult.result());
                      } else {
                        future.completeExceptionally(rowSetAsyncResult.cause());
                      }
                    }));
    result.get(30, TimeUnit.SECONDS);

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasKind(SpanKind.INTERNAL),
                span ->
                    span.hasName("SELECT tempdb.test")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(maybeStable(DB_NAME), DB),
                            equalTo(DB_USER, emitStableDatabaseSemconv() ? null : USER_DB),
                            equalTo(maybeStable(DB_STATEMENT), "select * from test"),
                            equalTo(maybeStable(DB_OPERATION), "SELECT"),
                            equalTo(maybeStable(DB_SQL_TABLE), "test"),
                            equalTo(SERVER_ADDRESS, host),
                            equalTo(SERVER_PORT, port)),
                span ->
                    span.hasName("callback")
                        .hasKind(SpanKind.INTERNAL)
                        .hasParent(trace.getSpan(0))));
  }

  @Test
  void testInvalidQuery() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    CompletableFuture<Object> result = new CompletableFuture<>();
    result.whenComplete(
        (rows, throwable) -> testing.runWithSpan("callback", () -> latch.countDown()));
    testing.runWithSpan(
        "parent",
        () ->
            pool.query("invalid")
                .execute(
                    rowSetAsyncResult -> {
                      if (rowSetAsyncResult.succeeded()) {
                        result.complete(rowSetAsyncResult.result());
                      } else {
                        result.completeExceptionally(rowSetAsyncResult.cause());
                      }
                    }));

    latch.await(30, TimeUnit.SECONDS);

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasKind(SpanKind.INTERNAL),
                span ->
                    span.hasName("tempdb")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasStatus(StatusData.error())
                        .hasEventsSatisfyingExactly(
                            event ->
                                event
                                    .hasName("exception")
                                    .hasAttributesSatisfyingExactly(
                                        equalTo(EXCEPTION_TYPE, PgException.class.getName()),
                                        satisfies(
                                            EXCEPTION_MESSAGE,
                                            val -> val.contains("syntax error at or near")),
                                        satisfies(
                                            EXCEPTION_STACKTRACE,
                                            val -> val.isInstanceOf(String.class))))
                        .hasAttributesSatisfyingExactly(
                            equalTo(maybeStable(DB_NAME), DB),
                            equalTo(DB_USER, emitStableDatabaseSemconv() ? null : USER_DB),
                            equalTo(maybeStable(DB_STATEMENT), "invalid"),
                            equalTo(DB_RESPONSE_STATUS_CODE, "42601"),
                            equalTo(ERROR_TYPE, "io.vertx.pgclient.PgException"),
                            equalTo(SERVER_ADDRESS, host),
                            equalTo(SERVER_PORT, port)),
                span ->
                    span.hasName("callback")
                        .hasKind(SpanKind.INTERNAL)
                        .hasParent(trace.getSpan(0))));
  }

  @Test
  void testPreparedSelect() throws Exception {
    testing
        .runWithSpan(
            "parent",
            () -> pool.preparedQuery("select * from test where id = $1").execute(Tuple.of(1)))
        .toCompletionStage()
        .toCompletableFuture()
        .get(30, TimeUnit.SECONDS);

    assertPreparedSelect();
  }

  private static void assertPreparedSelect() {
    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasKind(SpanKind.INTERNAL),
                span ->
                    span.hasName("SELECT tempdb.test")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(maybeStable(DB_NAME), DB),
                            equalTo(DB_USER, emitStableDatabaseSemconv() ? null : USER_DB),
                            equalTo(maybeStable(DB_STATEMENT), "select * from test where id = $1"),
                            equalTo(maybeStable(DB_OPERATION), "SELECT"),
                            equalTo(maybeStable(DB_SQL_TABLE), "test"),
                            equalTo(SERVER_ADDRESS, host),
                            equalTo(SERVER_PORT, port))));
  }

  @Test
  void testBatch() throws Exception {
    testing
        .runWithSpan(
            "parent",
            () ->
                pool.preparedQuery("insert into test values ($1, $2) returning *")
                    .executeBatch(Arrays.asList(Tuple.of(3, "Three"), Tuple.of(4, "Four"))))
        .toCompletionStage()
        .toCompletableFuture()
        .get(30, TimeUnit.SECONDS);

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasKind(SpanKind.INTERNAL),
                span ->
                    span.hasName("INSERT tempdb.test")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(maybeStable(DB_NAME), DB),
                            equalTo(DB_USER, emitStableDatabaseSemconv() ? null : USER_DB),
                            equalTo(
                                maybeStable(DB_STATEMENT),
                                "insert into test values ($1, $2) returning *"),
                            equalTo(maybeStable(DB_OPERATION), "INSERT"),
                            equalTo(maybeStable(DB_SQL_TABLE), "test"),
                            equalTo(SERVER_ADDRESS, host),
                            equalTo(SERVER_PORT, port))));
  }

  @Test
  void testWithTransaction() throws Exception {
    testing
        .runWithSpan(
            "parent",
            () ->
                pool.withTransaction(
                    conn ->
                        conn.preparedQuery("select * from test where id = $1")
                            .execute(Tuple.of(1))))
        .toCompletionStage()
        .toCompletableFuture()
        .get(30, TimeUnit.SECONDS);

    assertPreparedSelect();
  }

  @Test
  void testWithConnection() throws Exception {
    testing
        .runWithSpan(
            "parent",
            () ->
                pool.withConnection(
                    conn ->
                        conn.preparedQuery("select * from test where id = $1")
                            .execute(Tuple.of(1))))
        .toCompletionStage()
        .toCompletableFuture()
        .get(30, TimeUnit.SECONDS);

    assertPreparedSelect();
  }

  @Test
  void testManyQueries() throws Exception {
    int count = 50;
    CountDownLatch latch = new CountDownLatch(count);
    List<CompletableFuture<Object>> futureList = new ArrayList<>();
    List<CompletableFuture<Object>> resultList = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      CompletableFuture<Object> future = new CompletableFuture<>();
      futureList.add(future);
      resultList.add(
          future.whenComplete((rows, throwable) -> testing.runWithSpan("callback", () -> {})));
    }
    for (CompletableFuture<Object> future : futureList) {
      testing.runWithSpan(
          "parent",
          () ->
              pool.query("select * from test")
                  .execute(
                      rowSetAsyncResult -> {
                        if (rowSetAsyncResult.succeeded()) {
                          future.complete(rowSetAsyncResult.result());
                        } else {
                          future.completeExceptionally(rowSetAsyncResult.cause());
                        }
                        latch.countDown();
                      }));
    }
    latch.await(30, TimeUnit.SECONDS);
    for (CompletableFuture<Object> result : resultList) {
      result.get(10, TimeUnit.SECONDS);
    }

    List<Consumer<TraceAssert>> assertions =
        Collections.nCopies(
            count,
            trace ->
                trace.hasSpansSatisfyingExactly(
                    span -> span.hasName("parent").hasKind(SpanKind.INTERNAL),
                    span ->
                        span.hasName("SELECT tempdb.test")
                            .hasKind(SpanKind.CLIENT)
                            .hasParent(trace.getSpan(0))
                            .hasAttributesSatisfyingExactly(
                                equalTo(maybeStable(DB_NAME), DB),
                                equalTo(DB_USER, emitStableDatabaseSemconv() ? null : USER_DB),
                                equalTo(maybeStable(DB_STATEMENT), "select * from test"),
                                equalTo(maybeStable(DB_OPERATION), "SELECT"),
                                equalTo(maybeStable(DB_SQL_TABLE), "test"),
                                equalTo(SERVER_ADDRESS, host),
                                equalTo(SERVER_PORT, port)),
                    span ->
                        span.hasName("callback")
                            .hasKind(SpanKind.INTERNAL)
                            .hasParent(trace.getSpan(0))));
    testing.waitAndAssertTraces(assertions);
  }

  @Test
  void testConcurrency() throws Exception {
    int count = 50;
    CountDownLatch latch = new CountDownLatch(count);
    List<CompletableFuture<Object>> futureList = new ArrayList<>();
    List<CompletableFuture<Object>> resultList = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      CompletableFuture<Object> future = new CompletableFuture<>();
      futureList.add(future);
      resultList.add(
          future.whenComplete((rows, throwable) -> testing.runWithSpan("callback", () -> {})));
    }
    ExecutorService executorService = Executors.newFixedThreadPool(4);
    cleanup.deferCleanup(() -> executorService.shutdown());
    for (CompletableFuture<Object> future : futureList) {
      executorService.submit(
          () -> {
            testing.runWithSpan(
                "parent",
                () ->
                    pool.withConnection(
                            conn ->
                                conn.preparedQuery("select * from test where id = $1")
                                    .execute(Tuple.of(1)))
                        .onComplete(
                            rowSetAsyncResult -> {
                              if (rowSetAsyncResult.succeeded()) {
                                future.complete(rowSetAsyncResult.result());
                              } else {
                                future.completeExceptionally(rowSetAsyncResult.cause());
                              }
                              latch.countDown();
                            }));
          });
    }
    latch.await(30, TimeUnit.SECONDS);
    for (CompletableFuture<Object> result : resultList) {
      result.get(10, TimeUnit.SECONDS);
    }

    List<Consumer<TraceAssert>> assertions =
        Collections.nCopies(
            count,
            trace ->
                trace.hasSpansSatisfyingExactly(
                    span -> span.hasName("parent").hasKind(SpanKind.INTERNAL),
                    span ->
                        span.hasName("SELECT tempdb.test")
                            .hasKind(SpanKind.CLIENT)
                            .hasParent(trace.getSpan(0))
                            .hasAttributesSatisfyingExactly(
                                equalTo(maybeStable(DB_NAME), DB),
                                equalTo(DB_USER, emitStableDatabaseSemconv() ? null : USER_DB),
                                equalTo(
                                    maybeStable(DB_STATEMENT), "select * from test where id = $1"),
                                equalTo(maybeStable(DB_OPERATION), "SELECT"),
                                equalTo(maybeStable(DB_SQL_TABLE), "test"),
                                equalTo(SERVER_ADDRESS, host),
                                equalTo(SERVER_PORT, port)),
                    span ->
                        span.hasName("callback")
                            .hasKind(SpanKind.INTERNAL)
                            .hasParent(trace.getSpan(0))));
    testing.waitAndAssertTraces(assertions);
  }
}
