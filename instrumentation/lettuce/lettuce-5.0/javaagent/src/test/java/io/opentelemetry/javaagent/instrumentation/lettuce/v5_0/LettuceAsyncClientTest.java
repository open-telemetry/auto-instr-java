/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.lettuce.v5_0;

import static io.opentelemetry.api.common.AttributeKey.booleanKey;
import static io.opentelemetry.instrumentation.testing.junit.db.SemconvStabilityUtil.maybeStable;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.satisfies;
import static io.opentelemetry.semconv.ErrorAttributes.ERROR_TYPE;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_ADDRESS;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_PORT;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_OPERATION;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_STATEMENT;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_SYSTEM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchException;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.awaitility.Awaitility.await;

import com.google.common.collect.ImmutableMap;
import io.lettuce.core.ConnectionFuture;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.RedisURI;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.Utf8StringCodec;
import io.lettuce.core.protocol.AsyncCommand;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.test.utils.PortUtils;
import io.opentelemetry.sdk.trace.data.StatusData;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import org.assertj.core.api.AbstractAssert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

@SuppressWarnings("deprecation") // using deprecated semconv
class LettuceAsyncClientTest extends AbstractLettuceClientTest {
  private static int incorrectPort;
  private static String dbUriNonExistent;

  private static final ImmutableMap<String, String> testHashMap =
      ImmutableMap.of(
          "firstname", "John",
          "lastname", "Doe",
          "age", "53");

  private static RedisAsyncCommands<String, String> asyncCommands;

  @BeforeAll
  static void setUp() throws UnknownHostException {
    redisServer.start();

    host = redisServer.getHost();
    ip = InetAddress.getByName(host).getHostAddress();
    port = redisServer.getMappedPort(6379);
    embeddedDbUri = "redis://" + host + ":" + port + "/" + DB_INDEX;

    incorrectPort = PortUtils.findOpenPort();
    dbUriNonExistent = "redis://" + host + ":" + incorrectPort + "/" + DB_INDEX;

    redisClient = RedisClient.create(embeddedDbUri);
    redisClient.setOptions(CLIENT_OPTIONS);

    connection = redisClient.connect();
    asyncCommands = connection.async();
    RedisCommands<String, String> syncCommands = connection.sync();

    syncCommands.set("TESTKEY", "TESTVAL");

    // 1 set + 1 connect trace
    testing.waitForTraces(2);
    testing.clearData();
  }

  @AfterAll
  static void cleanUp() {
    connection.close();
    shutdown(redisClient);
    redisServer.stop();
  }

  @SuppressWarnings("deprecation") // RedisURI constructor
  @Test
  void testConnectUsingGetOnConnectionFuture() throws ExecutionException, InterruptedException {
    RedisClient testConnectionClient = RedisClient.create(embeddedDbUri);
    testConnectionClient.setOptions(CLIENT_OPTIONS);

    ConnectionFuture<StatefulRedisConnection<String, String>> connectionFuture =
        testConnectionClient.connectAsync(
            new Utf8StringCodec(), new RedisURI(host, port, 3, TimeUnit.SECONDS));
    StatefulRedisConnection<String, String> connection1 = connectionFuture.get();
    cleanup.deferCleanup(connection1);
    cleanup.deferCleanup(testConnectionClient::shutdown);

    assertThat(connection1).isNotNull();

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("CONNECT")
                        .hasKind(SpanKind.CLIENT)
                        .hasAttributesSatisfyingExactly(
                            equalTo(SERVER_ADDRESS, host),
                            equalTo(SERVER_PORT, port),
                            equalTo(maybeStable(DB_SYSTEM), "redis"))));
  }

  @SuppressWarnings("deprecation") // RedisURI constructor
  @Test
  void testConnectExceptionInsideTheConnectionFuture() {
    RedisClient testConnectionClient = RedisClient.create(dbUriNonExistent);
    testConnectionClient.setOptions(CLIENT_OPTIONS);

    Exception exception =
        catchException(
            () -> {
              ConnectionFuture<StatefulRedisConnection<String, String>> connectionFuture =
                  testConnectionClient.connectAsync(
                      new Utf8StringCodec(),
                      new RedisURI(host, incorrectPort, 3, TimeUnit.SECONDS));
              connectionFuture.get();
            });

    assertThat(exception).isInstanceOf(ExecutionException.class);

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("CONNECT")
                        .hasKind(SpanKind.CLIENT)
                        .hasStatus(StatusData.error())
                        .hasAttributesSatisfyingExactly(
                            equalTo(SERVER_ADDRESS, host),
                            equalTo(SERVER_PORT, incorrectPort),
                            equalTo(maybeStable(DB_SYSTEM), "redis"))
                        .hasEventsSatisfyingExactly(
                            event ->
                                event
                                    .hasName("exception")
                                    .hasAttributesSatisfyingExactly(
                                        equalTo(
                                            AttributeKey.stringKey("exception.type"),
                                            "io.netty.channel.AbstractChannel.AnnotatedConnectException"),
                                        equalTo(
                                            AttributeKey.stringKey("exception.message"),
                                            "Connection refused: "
                                                + host
                                                + "/"
                                                + ip
                                                + ":"
                                                + incorrectPort),
                                        satisfies(
                                            AttributeKey.stringKey("exception.stacktrace"),
                                            AbstractAssert::isNotNull)))));
  }

  @Test
  void testSetCommandUsingFutureGetWithTimeout()
      throws ExecutionException, InterruptedException, TimeoutException {
    RedisFuture<String> redisFuture = asyncCommands.set("TESTSETKEY", "TESTSETVAL");
    String res = redisFuture.get(3, TimeUnit.SECONDS);

    assertThat(res).isEqualTo("OK");

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("SET")
                        .hasKind(SpanKind.CLIENT)
                        .hasAttributesSatisfyingExactly(
                            equalTo(maybeStable(DB_SYSTEM), "redis"),
                            equalTo(maybeStable(DB_STATEMENT), "SET TESTSETKEY ?"),
                            equalTo(maybeStable(DB_OPERATION), "SET"))));
  }

  @Test
  void testGetCommandChainedWithThenAccept()
      throws ExecutionException, InterruptedException, TimeoutException {
    CompletableFuture<String> future = new CompletableFuture<>();
    Consumer<String> consumer =
        res -> {
          testing.runWithSpan("callback", () -> assertThat(res).isEqualTo("TESTVAL"));
          future.complete(res);
        };

    testing.runWithSpan(
        "parent",
        () -> {
          RedisFuture<String> redisFuture = asyncCommands.get("TESTKEY");
          redisFuture.thenAccept(consumer);
        });

    assertThat(future.get(10, TimeUnit.SECONDS)).isEqualTo("TESTVAL");
    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasKind(SpanKind.INTERNAL).hasNoParent(),
                span ->
                    span.hasName("GET")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(maybeStable(DB_SYSTEM), "redis"),
                            equalTo(maybeStable(DB_STATEMENT), "GET TESTKEY"),
                            equalTo(maybeStable(DB_OPERATION), "GET")),
                span ->
                    span.hasName("callback")
                        .hasKind(SpanKind.INTERNAL)
                        .hasParent(trace.getSpan(0))));
  }

  // to make sure instrumentation's chained completion stages won't interfere with user's, while
  // still recording spans
  @Test
  void testGetNonExistentKeyCommandWithHandleAsyncAndChainedWithThenApply()
      throws ExecutionException, InterruptedException, TimeoutException {
    CompletableFuture<String> future = new CompletableFuture<>();

    String successStr = "KEY MISSING";

    BiFunction<String, Throwable, String> firstStage =
        (res, error) -> {
          testing.runWithSpan(
              "callback1",
              () -> {
                assertThat(res).isNull();
                assertThat(error).isNull();
              });
          return (res == null ? successStr : res);
        };
    Function<String, Object> secondStage =
        input -> {
          testing.runWithSpan(
              "callback2",
              () -> {
                assertThat(input).isEqualTo(successStr);
                future.complete(successStr);
              });
          return null;
        };

    testing.runWithSpan(
        "parent",
        () -> {
          RedisFuture<String> redisFuture = asyncCommands.get("NON_EXISTENT_KEY");
          redisFuture.handle(firstStage).thenApply(secondStage);
        });

    assertThat(future.get(10, TimeUnit.SECONDS)).isEqualTo(successStr);

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasKind(SpanKind.INTERNAL).hasNoParent(),
                span ->
                    span.hasName("GET")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(maybeStable(DB_SYSTEM), "redis"),
                            equalTo(maybeStable(DB_STATEMENT), "GET NON_EXISTENT_KEY"),
                            equalTo(maybeStable(DB_OPERATION), "GET")),
                span ->
                    span.hasName("callback1")
                        .hasKind(SpanKind.INTERNAL)
                        .hasParent(trace.getSpan(0)),
                span ->
                    span.hasName("callback2")
                        .hasKind(SpanKind.INTERNAL)
                        .hasParent(trace.getSpan(0))));
  }

  @Test
  void testCommandWithNoArgumentsUsingBiconsumer()
      throws ExecutionException, InterruptedException, TimeoutException {
    CompletableFuture<String> future = new CompletableFuture<>();
    BiConsumer<String, Throwable> biConsumer =
        (keyRetrieved, error) ->
            testing.runWithSpan(
                "callback",
                () -> {
                  assertThat(keyRetrieved).isNotNull();
                  future.complete(keyRetrieved);
                });

    testing.runWithSpan(
        "parent",
        () -> {
          RedisFuture<String> redisFuture = asyncCommands.randomkey();
          redisFuture.whenCompleteAsync(biConsumer);
        });

    assertThat(future.get(10, TimeUnit.SECONDS)).isNotNull();
    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasKind(SpanKind.INTERNAL).hasNoParent(),
                span ->
                    span.hasName("RANDOMKEY")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(maybeStable(DB_SYSTEM), "redis"),
                            equalTo(maybeStable(DB_STATEMENT), "RANDOMKEY"),
                            equalTo(maybeStable(DB_OPERATION), "RANDOMKEY")),
                span ->
                    span.hasName("callback")
                        .hasKind(SpanKind.INTERNAL)
                        .hasParent(trace.getSpan(0))));
  }

  @Test
  void testHashSetAndThenNestApplyToHashGetall()
      throws ExecutionException, InterruptedException, TimeoutException {
    CompletableFuture<Map<String, String>> future = new CompletableFuture<>();

    RedisFuture<String> hmsetFuture = asyncCommands.hmset("TESTHM", testHashMap);
    hmsetFuture.thenApplyAsync(
        setResult -> {
          // Wait for 'hmset' trace to get written
          testing.waitForTraces(1);

          if (!"OK".equals(setResult)) {
            future.completeExceptionally(new AssertionError("Wrong hmset result " + setResult));
            return null;
          }

          RedisFuture<Map<String, String>> hmGetAllFuture = asyncCommands.hgetall("TESTHM");
          hmGetAllFuture.whenComplete(
              (result, exception) -> {
                if (exception != null) {
                  future.completeExceptionally(exception);
                } else {
                  future.complete(result);
                }
              });
          return null;
        });

    assertThat(future.get(10, TimeUnit.SECONDS)).isEqualTo(testHashMap);

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("HMSET")
                        .hasKind(SpanKind.CLIENT)
                        .hasAttributesSatisfyingExactly(
                            equalTo(maybeStable(DB_SYSTEM), "redis"),
                            equalTo(
                                maybeStable(DB_STATEMENT),
                                "HMSET TESTHM firstname ? lastname ? age ?"),
                            equalTo(maybeStable(DB_OPERATION), "HMSET"))),
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("HGETALL")
                        .hasKind(SpanKind.CLIENT)
                        .hasAttributesSatisfyingExactly(
                            equalTo(maybeStable(DB_SYSTEM), "redis"),
                            equalTo(maybeStable(DB_STATEMENT), "HGETALL TESTHM"),
                            equalTo(maybeStable(DB_OPERATION), "HGETALL"))));
  }

  @Test
  void testCommandCompletesExceptionally() {
    // turn off auto flush to complete the command exceptionally manually
    asyncCommands.setAutoFlushCommands(false);
    cleanup.deferCleanup(() -> asyncCommands.setAutoFlushCommands(true));

    RedisFuture<Long> redisFuture = asyncCommands.del("key1", "key2");
    boolean completedExceptionally =
        ((AsyncCommand<?, ?, ?>) redisFuture)
            .completeExceptionally(new IllegalStateException("TestException"));

    redisFuture.exceptionally(
        error -> {
          assertThat(error).isNotNull();
          assertThat(error).isInstanceOf(IllegalStateException.class);
          assertThat(error.getMessage()).isEqualTo("TestException");
          throw new RuntimeException(error);
        });

    asyncCommands.flushCommands();
    Throwable thrown = catchThrowable(redisFuture::get);

    await()
        .untilAsserted(
            () -> {
              assertThat(thrown).isInstanceOf(ExecutionException.class);
              assertThat(completedExceptionally).isTrue();
            });

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("DEL")
                        .hasKind(SpanKind.CLIENT)
                        .hasStatus(StatusData.error())
                        .hasException(new IllegalStateException("TestException"))
                        .hasAttributesSatisfyingExactly(
                            equalTo(maybeStable(DB_SYSTEM), "redis"),
                            equalTo(maybeStable(DB_STATEMENT), "DEL key1 key2"),
                            equalTo(maybeStable(DB_OPERATION), "DEL"),
                            equalTo(ERROR_TYPE, "java.lang.IllegalStateException"))));
  }

  @Test
  void testCancelCommandBeforeItFinishes() {
    asyncCommands.setAutoFlushCommands(false);
    cleanup.deferCleanup(() -> asyncCommands.setAutoFlushCommands(true));

    RedisFuture<Long> redisFuture =
        testing.runWithSpan("parent", () -> asyncCommands.sadd("SKEY", "1", "2"));
    redisFuture.whenCompleteAsync(
        (res, error) ->
            testing.runWithSpan(
                "callback",
                () -> {
                  assertThat(error).isNotNull();
                  assertThat(error).isInstanceOf(CancellationException.class);
                }));

    boolean cancelSuccess = redisFuture.cancel(true);
    asyncCommands.flushCommands();

    await().untilAsserted(() -> assertThat(cancelSuccess).isTrue());
    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("parent")
                        .hasKind(SpanKind.INTERNAL)
                        .hasNoParent()
                        .hasAttributes(Attributes.empty()),
                span ->
                    span.hasName("SADD")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(maybeStable(DB_SYSTEM), "redis"),
                            equalTo(maybeStable(DB_STATEMENT), "SADD SKEY ? ?"),
                            equalTo(maybeStable(DB_OPERATION), "SADD"),
                            equalTo(booleanKey("lettuce.command.cancelled"), true)),
                span ->
                    span.hasName("callback")
                        .hasKind(SpanKind.INTERNAL)
                        .hasParent(trace.getSpan(0))));
  }

  @Test
  void testDebugSegfaultCommandWithNoArgumentShouldProduceSpan() {
    // Test Causes redis to crash therefore it needs its own container
    try (StatefulRedisConnection<String, String> statefulConnection = newContainerConnection()) {
      RedisAsyncCommands<String, String> commands = statefulConnection.async();
      commands.debugSegfault();
    }

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("DEBUG")
                        .hasKind(SpanKind.CLIENT)
                        .hasAttributesSatisfyingExactly(
                            equalTo(maybeStable(DB_SYSTEM), "redis"),
                            equalTo(maybeStable(DB_STATEMENT), "DEBUG SEGFAULT"),
                            equalTo(maybeStable(DB_OPERATION), "DEBUG"))));
  }

  @Test
  void testShutdownCommandShouldProduceSpan() {
    // Test Causes redis to crash therefore it needs its own container
    try (StatefulRedisConnection<String, String> statefulConnection = newContainerConnection()) {
      RedisAsyncCommands<String, String> commands = statefulConnection.async();
      commands.shutdown(false);
    }

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("SHUTDOWN")
                        .hasKind(SpanKind.CLIENT)
                        .hasAttributesSatisfyingExactly(
                            equalTo(maybeStable(DB_SYSTEM), "redis"),
                            equalTo(maybeStable(DB_STATEMENT), "SHUTDOWN NOSAVE"),
                            equalTo(maybeStable(DB_OPERATION), "SHUTDOWN"))));
  }
}
