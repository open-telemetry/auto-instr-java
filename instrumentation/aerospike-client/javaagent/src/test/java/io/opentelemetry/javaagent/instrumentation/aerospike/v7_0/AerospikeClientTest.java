/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.aerospike.v7_0;

import static io.opentelemetry.api.common.AttributeKey.longKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.opentelemetry.semconv.NetworkAttributes.NETWORK_PEER_ADDRESS;
import static io.opentelemetry.semconv.NetworkAttributes.NETWORK_PEER_PORT;
import static io.opentelemetry.semconv.NetworkAttributes.NETWORK_TYPE;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_NAME;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_OPERATION;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_SYSTEM;
import static java.util.Collections.singletonList;

import com.aerospike.client.AerospikeClient;
import com.aerospike.client.Bin;
import com.aerospike.client.Key;
import com.aerospike.client.Record;
import com.aerospike.client.async.EventPolicy;
import com.aerospike.client.async.NioEventLoops;
import com.aerospike.client.policy.ClientPolicy;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

@SuppressWarnings("deprecation") // using deprecated semconv
class AerospikeClientTest {
  @RegisterExtension
  static final InstrumentationExtension testing = AgentInstrumentationExtension.create();

  public static final AttributeKey<Long> AEROSPIKE_STATUS = longKey("db.status");
  public static final AttributeKey<String> AEROSPIKE_SET_NAME = stringKey("db.set.name");
  public static final AttributeKey<String> AEROSPIKE_NODE_NAME = stringKey("db.node.name");

  static GenericContainer<?> aerospikeServer =
      new GenericContainer<>("aerospike/aerospike-server:6.2.0.0")
          .withExposedPorts(3000)
          .waitingFor(Wait.forLogMessage(".*replication factor is 1.*", 1));

  static int port;

  static AerospikeClient aerospikeClient;

  @BeforeAll
  static void setupSpec() {
    aerospikeServer.start();
    port = 3000;
    ClientPolicy clientPolicy = new ClientPolicy();
    int eventLoopSize = Runtime.getRuntime().availableProcessors();
    EventPolicy eventPolicy = new EventPolicy();
    eventPolicy.commandsPerEventLoop = 2;
    clientPolicy.eventLoops = new NioEventLoops(eventPolicy, eventLoopSize);
    clientPolicy.maxConnsPerNode = eventLoopSize;
    clientPolicy.failIfNotConnected = true;
    aerospikeClient = new AerospikeClient(clientPolicy, "localhost", 3000);
  }

  @AfterAll
  static void cleanupSpec() {
    aerospikeClient.close();
    aerospikeServer.stop();
  }

  @BeforeEach
  void setup() {
    testing.clearData();
  }

  @Test
  void asyncCommand() {
    Key aerospikeKey = new Key("test", "test-set", "data1");
    aerospikeClient.put(null, null, null, aerospikeKey, new Bin("bin1", "value1"));

    AtomicReference<String> instrumentationName = new AtomicReference<>();
    testing.waitAndAssertTraces(
        trace -> {
          instrumentationName.set(trace.getSpan(0).getInstrumentationScopeInfo().getName());
          trace.hasSpansSatisfyingExactly(
              span ->
                  span.hasName("PUT test")
                      .hasKind(SpanKind.CLIENT)
                      .hasAttributesSatisfyingExactly(
                          equalTo(DB_SYSTEM, "aerospike"),
                          equalTo(DB_OPERATION, "PUT"),
                          equalTo(NETWORK_PEER_PORT, port),
                          equalTo(NETWORK_PEER_ADDRESS, "127.0.0.1"),
                          equalTo(NETWORK_TYPE, "ipv4"),
                          equalTo(DB_NAME, "test"),
                          equalTo(AEROSPIKE_NODE_NAME, "BB9040017AC4202"),
                          equalTo(AEROSPIKE_SET_NAME, "test-set"),
                          equalTo(AEROSPIKE_STATUS, 0)));
        });

    testing.waitAndAssertMetrics(
        instrumentationName.get(),
        "aerospike.concurrency",
        metrics ->
            metrics.anySatisfy(
                metric ->
                    assertThat(metric)
                        .hasLongSumSatisfying(
                            upDownCounter ->
                                upDownCounter.hasPointsSatisfying(
                                    point ->
                                        point
                                            .hasValue(0)
                                            .hasAttributesSatisfying(
                                                equalTo(DB_NAME, "test"),
                                                equalTo(AEROSPIKE_SET_NAME, "test-set"),
                                                equalTo(DB_OPERATION, "PUT"),
                                                equalTo(DB_SYSTEM, "aerospike"))))));

    testing.waitAndAssertMetrics(
        instrumentationName.get(),
        "aerospike.client.duration",
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
                                            equalTo(DB_NAME, "test"),
                                            equalTo(AEROSPIKE_NODE_NAME, "BB9040017AC4202"),
                                            equalTo(AEROSPIKE_SET_NAME, "test-set"),
                                            equalTo(DB_OPERATION, "PUT"),
                                            equalTo(DB_SYSTEM, "aerospike"),
                                            equalTo(AEROSPIKE_STATUS, 0),
                                            equalTo(NETWORK_PEER_ADDRESS, "127.0.0.1"),
                                            equalTo(NETWORK_PEER_PORT, 3000),
                                            equalTo(NETWORK_TYPE, "ipv4"))))));
  }

  @Test
  void syncCommand() {
    Key aerospikeKey = new Key("test", "test-set", "data2");
    aerospikeClient.put(null, aerospikeKey, new Bin("bin2", "value2"));
    List<String> bins = singletonList("bin2");
    Record aerospikeRecord = aerospikeClient.get(null, aerospikeKey, bins.toArray(new String[0]));
    assertThat(aerospikeRecord.getString("bin2")).isEqualTo("value2");

    AtomicReference<String> instrumentationName = new AtomicReference<>();

    testing.waitAndAssertTraces(
        trace -> {
          instrumentationName.set(trace.getSpan(0).getInstrumentationScopeInfo().getName());
          trace.hasSpansSatisfyingExactly(
              span ->
                  span.hasName("PUT test")
                      .hasKind(SpanKind.CLIENT)
                      .hasAttributesSatisfyingExactly(
                          equalTo(DB_SYSTEM, "aerospike"),
                          equalTo(DB_OPERATION, "PUT"),
                          equalTo(DB_NAME, "test"),
                          equalTo(AEROSPIKE_SET_NAME, "test-set"),
                          equalTo(AEROSPIKE_STATUS, 0),
                          equalTo(AEROSPIKE_NODE_NAME, "BB9040017AC4202"),
                          equalTo(NETWORK_PEER_ADDRESS, "127.0.0.1"),
                          equalTo(NETWORK_PEER_PORT, 3000),
                          equalTo(NETWORK_TYPE, "ipv4")));
        },
        trace -> {
          instrumentationName.set(trace.getSpan(0).getInstrumentationScopeInfo().getName());
          trace.hasSpansSatisfyingExactly(
              span ->
                  span.hasName("GET test")
                      .hasKind(SpanKind.CLIENT)
                      .hasAttributesSatisfyingExactly(
                          equalTo(DB_SYSTEM, "aerospike"),
                          equalTo(DB_OPERATION, "GET"),
                          equalTo(DB_NAME, "test"),
                          equalTo(AEROSPIKE_SET_NAME, "test-set"),
                          equalTo(AEROSPIKE_STATUS, 0),
                          equalTo(AEROSPIKE_NODE_NAME, "BB9040017AC4202"),
                          equalTo(NETWORK_PEER_ADDRESS, "127.0.0.1"),
                          equalTo(NETWORK_PEER_PORT, 3000),
                          equalTo(NETWORK_TYPE, "ipv4")));
        });

    testing.waitAndAssertMetrics(
        instrumentationName.get(),
        "aerospike.concurrency",
        metrics ->
            metrics.anySatisfy(
                metric ->
                    assertThat(metric)
                        .hasLongSumSatisfying(
                            upDownCounter ->
                                upDownCounter.hasPointsSatisfying(
                                    point ->
                                        point
                                            .hasValue(0)
                                            .hasAttributesSatisfying(
                                                equalTo(DB_NAME, "test"),
                                                equalTo(AEROSPIKE_SET_NAME, "test-set"),
                                                equalTo(DB_OPERATION, "PUT"),
                                                equalTo(DB_SYSTEM, "aerospike")),
                                    point ->
                                        point
                                            .hasValue(0)
                                            .hasAttributesSatisfying(
                                                equalTo(DB_NAME, "test"),
                                                equalTo(AEROSPIKE_SET_NAME, "test-set"),
                                                equalTo(DB_OPERATION, "GET"),
                                                equalTo(DB_SYSTEM, "aerospike"))))));

    testing.waitAndAssertMetrics(
        instrumentationName.get(),
        "aerospike.client.duration",
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
                                            equalTo(DB_SYSTEM, "aerospike"),
                                            equalTo(DB_NAME, "test"),
                                            equalTo(DB_OPERATION, "PUT"),
                                            equalTo(AEROSPIKE_SET_NAME, "test-set"),
                                            equalTo(AEROSPIKE_STATUS, 0),
                                            equalTo(NETWORK_PEER_ADDRESS, "127.0.0.1"),
                                            equalTo(NETWORK_PEER_PORT, 3000),
                                            equalTo(NETWORK_TYPE, "ipv4"),
                                            equalTo(AEROSPIKE_NODE_NAME, "BB9040017AC4202")),
                                    point ->
                                        point.hasAttributesSatisfying(
                                            equalTo(DB_NAME, "test"),
                                            equalTo(AEROSPIKE_SET_NAME, "test-set"),
                                            equalTo(DB_OPERATION, "GET"),
                                            equalTo(DB_SYSTEM, "aerospike"),
                                            equalTo(AEROSPIKE_STATUS, 0),
                                            equalTo(NETWORK_PEER_ADDRESS, "127.0.0.1"),
                                            equalTo(NETWORK_PEER_PORT, 3000),
                                            equalTo(NETWORK_TYPE, "ipv4"),
                                            equalTo(AEROSPIKE_NODE_NAME, "BB9040017AC4202"))))));
  }
}
