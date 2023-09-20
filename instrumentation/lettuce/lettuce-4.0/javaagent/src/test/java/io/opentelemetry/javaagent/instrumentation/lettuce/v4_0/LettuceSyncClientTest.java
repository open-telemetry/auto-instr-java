/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.lettuce.v4_0;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchException;

import com.google.common.collect.ImmutableMap;
import com.lambdaworks.redis.ClientOptions;
import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisConnectionException;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.sync.RedisCommands;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.test.utils.PortUtils;
import io.opentelemetry.instrumentation.testing.internal.AutoCleanupExtension;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.semconv.SemanticAttributes;
import java.util.Map;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

@SuppressWarnings("deprecation") // until old http semconv are dropped in 2.0
class LettuceSyncClientTest {

  @RegisterExtension
  protected static final InstrumentationExtension testing = AgentInstrumentationExtension.create();

  @RegisterExtension static final AutoCleanupExtension cleanup = AutoCleanupExtension.create();

  static final DockerImageName containerImage = DockerImageName.parse("redis:6.2.3-alpine");

  private static final int DB_INDEX = 0;

  // Disable auto reconnect, so we do not get stray traces popping up on server shutdown
  private static final ClientOptions CLIENT_OPTIONS =
      new ClientOptions.Builder().autoReconnect(false).build();

  private static final GenericContainer<?> redisServer =
      new GenericContainer<>(containerImage).withExposedPorts(6379);

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
  static RedisCommands<String, String> syncCommands;

  @BeforeAll
  static void setUp() {
    redisServer.start();

    host = redisServer.getHost();
    port = redisServer.getMappedPort(6379);
    embeddedDbUri = "redis://" + host + ":" + port + "/" + DB_INDEX;

    incorrectPort = PortUtils.findOpenPort();
    dbUriNonExistent = "redis://" + host + ":" + incorrectPort + "/" + DB_INDEX;

    redisClient = RedisClient.create(embeddedDbUri);

    connection = redisClient.connect();
    syncCommands = connection.sync();

    syncCommands.set("TESTKEY", "TESTVAL");
    syncCommands.hmset("TESTHM", testHashMap);

    // 2 sets + 1 connect trace
    testing.waitForTraces(3);
    testing.clearData();
  }

  @AfterAll
  static void cleanUp() {
    connection.close();
    redisServer.stop();
  }

  @Test
  void testConnect() {
    RedisClient testConnectionClient = RedisClient.create(embeddedDbUri);
    testConnectionClient.setOptions(CLIENT_OPTIONS);

    StatefulRedisConnection<String, String> testConnection = testConnectionClient.connect();
    cleanup.deferCleanup(() -> testConnection.close());

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("CONNECT")
                        .hasKind(SpanKind.CLIENT)
                        .hasAttributesSatisfyingExactly(
                            equalTo(SemanticAttributes.NET_PEER_NAME, host),
                            equalTo(SemanticAttributes.NET_PEER_PORT, port),
                            equalTo(SemanticAttributes.DB_SYSTEM, "redis"))));
  }

  @Test
  void testConnectException() {
    RedisClient testConnectionClient = RedisClient.create(dbUriNonExistent);
    testConnectionClient.setOptions(CLIENT_OPTIONS);

    Exception exception = catchException(testConnectionClient::connect);

    assertThat(exception).isInstanceOf(RedisConnectionException.class);

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("CONNECT")
                        .hasKind(SpanKind.CLIENT)
                        .hasException(exception)
                        .hasAttributesSatisfyingExactly(
                            equalTo(SemanticAttributes.NET_PEER_NAME, host),
                            equalTo(SemanticAttributes.NET_PEER_PORT, incorrectPort),
                            equalTo(SemanticAttributes.DB_SYSTEM, "redis"))));
  }

  @Test
  void testSetCommand() {
    String res = syncCommands.set("TESTSETKEY", "TESTSETVAL");
    assertThat(res).isEqualTo("OK");

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("SET")
                        .hasKind(SpanKind.CLIENT)
                        .hasAttributesSatisfyingExactly(
                            equalTo(SemanticAttributes.DB_SYSTEM, "redis"),
                            equalTo(SemanticAttributes.DB_OPERATION, "SET"))));
  }

  @Test
  void testGetCommand() {
    String res = syncCommands.get("TESTKEY");
    assertThat(res).isEqualTo("TESTVAL");

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("GET")
                        .hasKind(SpanKind.CLIENT)
                        .hasAttributesSatisfyingExactly(
                            equalTo(SemanticAttributes.DB_SYSTEM, "redis"),
                            equalTo(SemanticAttributes.DB_OPERATION, "GET"))));
  }

  @Test
  void testGetNonExistentKeyCommand() {
    String res = syncCommands.get("NON_EXISTENT_KEY");
    assertThat(res).isNull();

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("GET")
                        .hasKind(SpanKind.CLIENT)
                        .hasAttributesSatisfyingExactly(
                            equalTo(SemanticAttributes.DB_SYSTEM, "redis"),
                            equalTo(SemanticAttributes.DB_OPERATION, "GET"))));
  }

  @Test
  void testCommandWithNoArguments() {
    String res = syncCommands.randomkey();
    assertThat(res).isNotNull();

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("RANDOMKEY")
                        .hasKind(SpanKind.CLIENT)
                        .hasAttributesSatisfyingExactly(
                            equalTo(SemanticAttributes.DB_SYSTEM, "redis"),
                            equalTo(SemanticAttributes.DB_OPERATION, "RANDOMKEY"))));
  }

  @Test
  void testListCommand() {
    long res = syncCommands.lpush("TESTLIST", "TESTLIST ELEMENT");
    assertThat(res).isEqualTo(1);

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("LPUSH")
                        .hasKind(SpanKind.CLIENT)
                        .hasAttributesSatisfyingExactly(
                            equalTo(SemanticAttributes.DB_SYSTEM, "redis"),
                            equalTo(SemanticAttributes.DB_OPERATION, "LPUSH"))));
  }

  @Test
  void testHashSetCommand() {
    String res = syncCommands.hmset("user", testHashMap);
    assertThat(res).isEqualTo("OK");

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("HMSET")
                        .hasKind(SpanKind.CLIENT)
                        .hasAttributesSatisfyingExactly(
                            equalTo(SemanticAttributes.DB_SYSTEM, "redis"),
                            equalTo(SemanticAttributes.DB_OPERATION, "HMSET"))));
  }

  @Test
  void testHashGetallCommand() {
    Map<String, String> res = syncCommands.hgetall("TESTHM");
    assertThat(res).isEqualTo(testHashMap);

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("HGETALL")
                        .hasKind(SpanKind.CLIENT)
                        .hasAttributesSatisfyingExactly(
                            equalTo(SemanticAttributes.DB_SYSTEM, "redis"),
                            equalTo(SemanticAttributes.DB_OPERATION, "HGETALL"))));
  }

  @Test
  void testDebugSegfaultCommandWithNoArgumentShouldProduceSpan() {
    // Test Causes redis to crash therefore it needs its own container
    GenericContainer<?> server = new GenericContainer<>(containerImage).withExposedPorts(6379);
    server.start();
    cleanup.deferCleanup(server::stop);

    long serverPort = server.getMappedPort(6379);
    RedisClient client = RedisClient.create("redis://" + host + ":" + serverPort + "/" + DB_INDEX);
    StatefulRedisConnection<String, String> connection1 = client.connect();
    cleanup.deferCleanup(connection1);

    RedisCommands<String, String> commands = connection1.sync();
    // 1 connect trace
    testing.clearData();

    commands.debugSegfault();

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("DEBUG")
                        .hasKind(SpanKind.CLIENT)
                        .hasAttributesSatisfyingExactly(
                            equalTo(SemanticAttributes.DB_SYSTEM, "redis"),
                            equalTo(SemanticAttributes.DB_OPERATION, "DEBUG"))));
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
    StatefulRedisConnection<String, String> connection1 = client.connect();
    cleanup.deferCleanup(connection1);

    RedisCommands<String, String> commands = connection1.sync();
    // 1 connect trace
    testing.clearData();

    commands.shutdown(false);

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("SHUTDOWN")
                        .hasKind(SpanKind.CLIENT)
                        .hasAttributesSatisfyingExactly(
                            equalTo(SemanticAttributes.DB_SYSTEM, "redis"),
                            equalTo(SemanticAttributes.DB_OPERATION, "SHUTDOWN"))));
  }
}
