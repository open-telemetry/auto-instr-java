/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.lettuce.v4_0;

import static io.opentelemetry.api.common.AttributeKey.booleanKey;
import static io.opentelemetry.instrumentation.testing.junit.db.SemconvStabilityUtil.maybeStable;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.opentelemetry.semconv.ErrorAttributes.ERROR_TYPE;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_ADDRESS;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_PORT;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_OPERATION;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_SYSTEM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchException;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.awaitility.Awaitility.await;

import com.google.common.collect.ImmutableMap;
import com.lambdaworks.redis.ClientOptions;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisConnectionException;
import com.lambdaworks.redis.RedisFuture;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.async.RedisAsyncCommands;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.codec.Utf8StringCodec;
import com.lambdaworks.redis.protocol.AsyncCommand;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.test.utils.PortUtils;
import io.opentelemetry.instrumentation.testing.internal.AutoCleanupExtension;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.sdk.trace.data.StatusData;
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
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

@SuppressWarnings("deprecation") // using deprecated semconv
class LettuceAsyncClientTest {
  private static final Logger logger = LoggerFactory.getLogger(LettuceAsyncClientTest.class);

  @RegisterExtension
  protected static final InstrumentationExtension testing = AgentInstrumentationExtension.create();

  @RegisterExtension static final AutoCleanupExtension cleanup = AutoCleanupExtension.create();

  static final DockerImageName containerImage = DockerImageName.parse("redis:6.2.3-alpine");

  private static final int DB_INDEX = 0;

  // Disable auto reconnect, so we do not get stray traces popping up on server shutdown
  private static final ClientOptions CLIENT_OPTIONS =
      new ClientOptions.Builder().autoReconnect(false).build();

  private static final GenericContainer<?> redisServer =
      new GenericContainer<>(containerImage)
          .withExposedPorts(6379)
          .withLogConsumer(new Slf4jLogConsumer(logger))
          .waitingFor(Wait.forLogMessage(".*Ready to accept connections.*", 1));

  private static String host;
  private static int port;
  private static int incorrectPort;
  private static String dbUriNonExistent;
  private static String embeddedDbUri;

  private static final ImmutableMap<String, String> testHashMap =
      ImmutableMap.of(
          "firstname", "John",
          "lastname", "Doe",
          "age", "53");

  static RedisClient redisClient;
  private static StatefulRedisConnection<String, String> connection;
  static RedisAsyncCommands<String, String> asyncCommands;

  @BeforeAll
  static void setUp() {
    redisServer.start();
    host = redisServer.getHost();
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
    redisClient.shutdown();
    redisServer.stop();
  }

  @Test
  void testConnectUsingGetOnConnectionFuture() {
    RedisClient testConnectionClient = RedisClient.create(embeddedDbUri);
    testConnectionClient.setOptions(CLIENT_OPTIONS);

    StatefulRedisConnection<String, String> connection1 =
        testConnectionClient.connect(
            new Utf8StringCodec(), new RedisURI(host, port, 3, TimeUnit.SECONDS));
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

  @Test
  void testExceptionInsideTheConnectionFuture() {
    RedisClient testConnectionClient = RedisClient.create(dbUriNonExistent);
    testConnectionClient.setOptions(CLIENT_OPTIONS);
    cleanup.deferCleanup(testConnectionClient::shutdown);

    Exception exception =
        catchException(
            () ->
                testConnectionClient.connect(
                    new Utf8StringCodec(), new RedisURI(host, incorrectPort, 3, TimeUnit.SECONDS)));

    assertThat(exception).isInstanceOf(RedisConnectionException.class);

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("CONNECT")
                        .hasKind(SpanKind.CLIENT)
                        .hasStatus(StatusData.error())
                        .hasException(exception)
                        .hasAttributesSatisfyingExactly(
                            equalTo(SERVER_ADDRESS, host),
                            equalTo(SERVER_PORT, incorrectPort),
                            equalTo(maybeStable(DB_SYSTEM), "redis"))));
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
                            equalTo(maybeStable(DB_OPERATION), "SET"))));
  }

  @Test
  void testCommandChainedWithThenAccept() {
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

    await().untilAsserted(() -> assertThat(future).isCompletedWithValue("TESTVAL"));
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
                            equalTo(maybeStable(DB_OPERATION), "GET")),
                span ->
                    span.hasName("callback")
                        .hasKind(SpanKind.INTERNAL)
                        .hasParent(trace.getSpan(0))));
  }

  // to make sure instrumentation's chained completion stages won't interfere with user's, while
  // still recording spans
  @Test
  void getNonExistentKeyCommandWithHandleAsyncAndChainedWithThenApply() {
    CompletableFuture<String> future1 = new CompletableFuture<>();
    CompletableFuture<String> future2 = new CompletableFuture<>();

    String successStr = "KEY MISSING";

    BiFunction<String, Throwable, String> firstStage =
        (res, error) -> {
          testing.runWithSpan(
              "callback1",
              () -> {
                assertThat(res).isNull();
                assertThat(error).isNull();
                future1.complete(null);
              });
          return (res == null ? successStr : res);
        };
    Function<String, Object> secondStage =
        input -> {
          testing.runWithSpan(
              "callback2",
              () -> {
                assertThat(input).isEqualTo(successStr);
                future2.complete(successStr);
              });
          return null;
        };

    testing.runWithSpan(
        "parent",
        () -> {
          RedisFuture<String> redisFuture = asyncCommands.get("NON_EXISTENT_KEY");
          redisFuture.handle(firstStage).thenApply(secondStage);
        });

    await()
        .untilAsserted(
            () -> {
              assertThat(future1).isCompletedWithValue(null);
              assertThat(future2).isCompletedWithValue(successStr);
            });

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
  void testCommandWithNoArgumentsUsingBiconsumer() {
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

    await().untilAsserted(() -> assertThat(future).isCompleted());
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
                            equalTo(maybeStable(DB_OPERATION), "RANDOMKEY")),
                span ->
                    span.hasName("callback")
                        .hasKind(SpanKind.INTERNAL)
                        .hasParent(trace.getSpan(0))));
  }

  @Test
  void testHashSetAndThenNestApplyToHashGetall() {
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

    await().untilAsserted(() -> assertThat(future).isCompletedWithValue(testHashMap));

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("HMSET")
                        .hasKind(SpanKind.CLIENT)
                        .hasAttributesSatisfyingExactly(
                            equalTo(maybeStable(DB_SYSTEM), "redis"),
                            equalTo(maybeStable(DB_OPERATION), "HMSET"))),
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("HGETALL")
                        .hasKind(SpanKind.CLIENT)
                        .hasAttributesSatisfyingExactly(
                            equalTo(maybeStable(DB_SYSTEM), "redis"),
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
                            equalTo(maybeStable(DB_OPERATION), "DEL"),
                            equalTo(maybeStable(ERROR_TYPE), "java.lang.IllegalStateException"))));
  }

  @Test
  void testCommandBeforeItFinished() {
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
    GenericContainer<?> server = new GenericContainer<>(containerImage).withExposedPorts(6379);
    server.start();
    cleanup.deferCleanup(server::stop);

    long serverPort = server.getMappedPort(6379);
    RedisClient client = RedisClient.create("redis://" + host + ":" + serverPort + "/" + DB_INDEX);
    client.setOptions(CLIENT_OPTIONS);
    StatefulRedisConnection<String, String> connection1 = client.connect();
    cleanup.deferCleanup(connection1);
    cleanup.deferCleanup(client::shutdown);

    RedisAsyncCommands<String, String> commands = connection1.async();
    // 1 connect trace
    testing.waitForTraces(1);
    testing.clearData();

    commands.debugSegfault();

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("DEBUG")
                        .hasKind(SpanKind.CLIENT)
                        .hasAttributesSatisfyingExactly(
                            equalTo(maybeStable(DB_SYSTEM), "redis"),
                            equalTo(maybeStable(DB_OPERATION), "DEBUG"))));
  }

  @Test
  void testShutdownCommandShouldProduceSpan() {
    // Test Causes redis to crash therefore it needs its own container
    GenericContainer<?> server = new GenericContainer<>(containerImage).withExposedPorts(6379);
    server.start();
    cleanup.deferCleanup(server::stop);

    long shutdownServerPort = server.getMappedPort(6379);

    RedisClient client =
        RedisClient.create("redis://" + host + ":" + shutdownServerPort + "/" + DB_INDEX);
    client.setOptions(CLIENT_OPTIONS);
    StatefulRedisConnection<String, String> connection1 = client.connect();
    cleanup.deferCleanup(connection1);
    cleanup.deferCleanup(client::shutdown);

    RedisAsyncCommands<String, String> commands = connection1.async();
    // 1 connect trace
    testing.waitForTraces(1);
    testing.clearData();

    commands.shutdown(false);

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("SHUTDOWN")
                        .hasKind(SpanKind.CLIENT)
                        .hasAttributesSatisfyingExactly(
                            equalTo(maybeStable(DB_SYSTEM), "redis"),
                            equalTo(maybeStable(DB_OPERATION), "SHUTDOWN"))));
  }
}
