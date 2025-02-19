/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.spymemcached;

import static io.opentelemetry.api.common.AttributeKey.booleanKey;
import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.instrumentation.testing.junit.db.SemconvStabilityUtil.maybeStable;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.satisfies;
import static io.opentelemetry.semconv.ErrorAttributes.ERROR_TYPE;
import static io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_MESSAGE;
import static io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_STACKTRACE;
import static io.opentelemetry.semconv.ExceptionAttributes.EXCEPTION_TYPE;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_OPERATION;
import static io.opentelemetry.semconv.incubating.DbIncubatingAttributes.DB_SYSTEM;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static net.spy.memcached.ConnectionFactoryBuilder.Protocol.BINARY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.google.common.util.concurrent.MoreExecutors;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.testing.internal.AutoCleanupExtension;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.semconv.incubating.DbIncubatingAttributes;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import net.spy.memcached.CASResponse;
import net.spy.memcached.CASValue;
import net.spy.memcached.ConnectionFactory;
import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.DefaultConnectionFactory;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.internal.CheckedOperationTimeoutException;
import net.spy.memcached.ops.Operation;
import net.spy.memcached.ops.OperationQueueFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.testcontainers.containers.GenericContainer;

@SuppressWarnings("deprecation") // using deprecated semconv
class SpymemcachedTest {
  private static final String KEY_PREFIX = "SpymemcachedTest-";
  // https://github.com/memcached/memcached/wiki/Programming#expiration
  private static final int EXPIRATION_SECONDS = 3600;
  private static final int TIMING_OUT_OPERATION_TIMEOUT_MILLIS = 1000;

  @RegisterExtension
  static final InstrumentationExtension testing = AgentInstrumentationExtension.create();

  @RegisterExtension static final AutoCleanupExtension cleanup = AutoCleanupExtension.create();

  static GenericContainer<?> memcachedContainer;
  static InetSocketAddress memcachedAddress;

  @BeforeAll
  static void setUp() {
    memcachedContainer =
        new GenericContainer<>("memcached:latest")
            .withExposedPorts(11211)
            .withStartupTimeout(Duration.ofMinutes(2));
    memcachedContainer.start();
    memcachedAddress =
        new InetSocketAddress(
            memcachedContainer.getHost(), memcachedContainer.getMappedPort(11211));
  }

  @AfterAll
  static void cleanUp() {
    memcachedContainer.stop();
  }

  private static MemcachedClient getMemcached() {
    return getMemcached(emptyMap(), builder -> {});
  }

  private static MemcachedClient getMemcached(Map<String, String> testData) {
    return getMemcached(testData, builder -> {});
  }

  private static MemcachedClient getMemcached(
      Map<String, String> testData, Consumer<ConnectionFactoryBuilder> customizer) {
    // Use direct executor service so our listeners finish in deterministic order
    ExecutorService listenerExecutorService = MoreExecutors.newDirectExecutorService();

    ConnectionFactoryBuilder connectionFactoryBuilder =
        new ConnectionFactoryBuilder()
            .setListenerExecutorService(listenerExecutorService)
            .setProtocol(BINARY);
    customizer.accept(connectionFactoryBuilder);

    ConnectionFactory connectionFactory = connectionFactoryBuilder.build();
    try {
      MemcachedClient memcached =
          new MemcachedClient(connectionFactory, Collections.singletonList(memcachedAddress));
      cleanup.deferCleanup(memcached::shutdown);

      testing.runWithSpan(
          "setup",
          () -> {
            for (Map.Entry<String, String> entry : testData.entrySet()) {
              if (!memcached.set(key(entry.getKey()), EXPIRATION_SECONDS, entry.getValue()).get()) {
                throw new IllegalStateException("Failed to set key " + entry.getKey());
              }
            }
          });
      testing.waitForTraces(1);
      testing.clearData();

      return memcached;
    } catch (Exception exception) {
      throw new IllegalStateException(exception);
    }
  }

  @Test
  void getHit() {
    MemcachedClient memcached = getMemcached(singletonMap("test-get", "get test"));
    testing.runWithSpan(
        "parent", () -> assertThat(memcached.get(key("test-get"))).isEqualTo("get test"));

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasNoParent().hasAttributes(Attributes.empty()),
                span ->
                    span.hasName("get")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.MEMCACHED),
                            equalTo(maybeStable(DB_OPERATION), "get"),
                            equalTo(stringKey("spymemcached.result"), "hit"))));
  }

  @Test
  void getMiss() {
    MemcachedClient memcached = getMemcached();
    testing.runWithSpan(
        "parent", () -> assertThat(memcached.get(key("test-get-key-that-doesn't-exist"))).isNull());

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasNoParent().hasAttributes(Attributes.empty()),
                span ->
                    span.hasName("get")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.MEMCACHED),
                            equalTo(maybeStable(DB_OPERATION), "get"),
                            equalTo(stringKey("spymemcached.result"), "miss"))));
  }

  @Test
  void getCancel() {
    ReentrantLock queueLock = new ReentrantLock();
    OperationQueueFactory lockableQueueFactory = () -> getLockableQueue(queueLock);
    MemcachedClient lockableMemcached =
        getMemcached(
            singletonMap("test-get", "get test"),
            builder -> builder.setOpQueueFactory(lockableQueueFactory));
    testing.runWithSpan(
        "parent",
        () -> {
          queueLock.lock();
          try {
            lockableMemcached.asyncGet(key("test-get")).cancel(true);
          } finally {
            queueLock.unlock();
          }
        });

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasNoParent().hasAttributes(Attributes.empty()),
                span ->
                    span.hasName("get")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.MEMCACHED),
                            equalTo(maybeStable(DB_OPERATION), "get"),
                            equalTo(booleanKey("spymemcached.command.cancelled"), true))));
  }

  @Test
  void getTimeout() throws InterruptedException {
    /*
    Not using runWithSpan since timeouts happen in separate thread
    and direct executor doesn't help to make sure that parent span finishes last.
    Instead run without parent span to have only 1 span to test with.
     */
    ReentrantLock queueLock = new ReentrantLock();
    OperationQueueFactory lockableQueueFactory = () -> getLockableQueue(queueLock);
    MemcachedClient timingOutMemcached =
        getMemcached(
            singletonMap("test-get", "get test"),
            builder ->
                builder
                    .setOpQueueFactory(lockableQueueFactory)
                    .setOpTimeout(TIMING_OUT_OPERATION_TIMEOUT_MILLIS));
    queueLock.lock();
    try {
      timingOutMemcached.asyncGet(key("test-get"));
      Thread.sleep(TIMING_OUT_OPERATION_TIMEOUT_MILLIS + 1000);
    } finally {
      queueLock.unlock();
    }

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("get")
                        .hasKind(SpanKind.CLIENT)
                        .hasNoParent()
                        .hasStatus(StatusData.error())
                        .hasEventsSatisfyingExactly(
                            event ->
                                event
                                    .hasName("exception")
                                    .hasAttributesSatisfyingExactly(
                                        equalTo(
                                            EXCEPTION_TYPE,
                                            CheckedOperationTimeoutException.class.getName()),
                                        equalTo(
                                            EXCEPTION_MESSAGE,
                                            "Operation timed out. - failing node: "
                                                + memcachedAddress),
                                        satisfies(
                                            EXCEPTION_STACKTRACE,
                                            val -> val.isInstanceOf(String.class))))
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.MEMCACHED),
                            equalTo(maybeStable(DB_OPERATION), "get"))));
  }

  @Test
  void bulkGet() {
    Map<String, String> testData = new HashMap<>();
    testData.put("test-get", "get test");
    testData.put("test-get-2", "get test 2");
    MemcachedClient memcached = getMemcached(testData);
    Map<String, Object> result =
        testing.runWithSpan("parent", () -> memcached.getBulk(key("test-get"), key("test-get-2")));
    assertThat(result)
        .hasSize(2)
        .containsEntry(key("test-get"), "get test")
        .containsEntry(key("test-get-2"), "get test 2");

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasNoParent().hasAttributes(Attributes.empty()),
                span ->
                    span.hasName("getBulk")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.MEMCACHED),
                            equalTo(maybeStable(DB_OPERATION), "getBulk"))));
  }

  @Test
  void set() throws Exception {
    MemcachedClient memcached = getMemcached();
    testing.runWithSpan(
        "parent",
        () -> {
          assertThat(memcached.set(key("test-set"), EXPIRATION_SECONDS, "bar").get()).isTrue();
        });

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasNoParent().hasAttributes(Attributes.empty()),
                span ->
                    span.hasName("set")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.MEMCACHED),
                            equalTo(maybeStable(DB_OPERATION), "set"))));
  }

  @Test
  void setCancel() {
    ReentrantLock queueLock = new ReentrantLock();
    OperationQueueFactory lockableQueueFactory = () -> getLockableQueue(queueLock);
    MemcachedClient lockableMemcached =
        getMemcached(emptyMap(), builder -> builder.setOpQueueFactory(lockableQueueFactory));
    testing.runWithSpan(
        "parent",
        () -> {
          queueLock.lock();
          try {
            assertThat(
                    lockableMemcached
                        .set(key("test-set-cancel"), EXPIRATION_SECONDS, "bar")
                        .cancel())
                .isTrue();
          } finally {
            queueLock.unlock();
          }
        });

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasNoParent().hasAttributes(Attributes.empty()),
                span ->
                    span.hasName("set")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.MEMCACHED),
                            equalTo(maybeStable(DB_OPERATION), "set"),
                            equalTo(booleanKey("spymemcached.command.cancelled"), true))));
  }

  @Test
  void add() throws Exception {
    MemcachedClient memcached = getMemcached();
    testing.runWithSpan(
        "parent",
        () -> {
          assertThat(memcached.add(key("test-add"), EXPIRATION_SECONDS, "add bar").get()).isTrue();
          assertThat(memcached.get(key("test-add"))).isEqualTo("add bar");
        });

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasNoParent().hasAttributes(Attributes.empty()),
                span ->
                    span.hasName("add")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.MEMCACHED),
                            equalTo(maybeStable(DB_OPERATION), "add")),
                span ->
                    span.hasName("get")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.MEMCACHED),
                            equalTo(maybeStable(DB_OPERATION), "get"),
                            equalTo(stringKey("spymemcached.result"), "hit"))));
  }

  @Test
  void secondAdd() throws Exception {
    MemcachedClient memcached = getMemcached();
    testing.runWithSpan(
        "parent",
        () -> {
          assertThat(memcached.add(key("test-add2"), EXPIRATION_SECONDS, "add bar").get()).isTrue();
          assertThat(memcached.add(key("test-add2"), EXPIRATION_SECONDS, "add bar 123").get())
              .isFalse();
        });

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasNoParent().hasAttributes(Attributes.empty()),
                span ->
                    span.hasName("add")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.MEMCACHED),
                            equalTo(maybeStable(DB_OPERATION), "add")),
                span ->
                    span.hasName("add")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.MEMCACHED),
                            equalTo(maybeStable(DB_OPERATION), "add"))));
  }

  @Test
  void delete() throws Exception {
    MemcachedClient memcached = getMemcached(singletonMap("test-delete", "delete test"));
    testing.runWithSpan(
        "parent",
        () -> {
          assertThat(memcached.delete(key("test-delete")).get()).isTrue();
          assertThat(memcached.get(key("test-delete"))).isNull();
        });

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasNoParent().hasAttributes(Attributes.empty()),
                span ->
                    span.hasName("delete")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.MEMCACHED),
                            equalTo(maybeStable(DB_OPERATION), "delete")),
                span ->
                    span.hasName("get")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.MEMCACHED),
                            equalTo(maybeStable(DB_OPERATION), "get"),
                            equalTo(stringKey("spymemcached.result"), "miss"))));
  }

  @Test
  void deleteNonExistent() throws Exception {
    MemcachedClient memcached = getMemcached();
    testing.runWithSpan(
        "parent",
        () -> {
          assertThat(memcached.delete(key("test-delete-non-existent")).get()).isFalse();
        });

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasNoParent().hasAttributes(Attributes.empty()),
                span ->
                    span.hasName("delete")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.MEMCACHED),
                            equalTo(maybeStable(DB_OPERATION), "delete"))));
  }

  @Test
  void replace() throws Exception {
    MemcachedClient memcached = getMemcached(singletonMap("test-replace", "replace test"));
    testing.runWithSpan(
        "parent",
        () -> {
          assertThat(memcached.replace(key("test-replace"), EXPIRATION_SECONDS, "new value").get())
              .isTrue();
          assertThat(memcached.get(key("test-replace"))).isEqualTo("new value");
        });

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasNoParent().hasAttributes(Attributes.empty()),
                span ->
                    span.hasName("replace")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.MEMCACHED),
                            equalTo(maybeStable(DB_OPERATION), "replace")),
                span ->
                    span.hasName("get")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.MEMCACHED),
                            equalTo(maybeStable(DB_OPERATION), "get"),
                            equalTo(stringKey("spymemcached.result"), "hit"))));
  }

  @Test
  void replaceNonExistent() throws Exception {
    MemcachedClient memcached = getMemcached();
    testing.runWithSpan(
        "parent",
        () -> {
          assertThat(
                  memcached
                      .replace(key("test-replace-non-existent"), EXPIRATION_SECONDS, "new value")
                      .get())
              .isFalse();
        });

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasNoParent().hasAttributes(Attributes.empty()),
                span ->
                    span.hasName("replace")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.MEMCACHED),
                            equalTo(maybeStable(DB_OPERATION), "replace"))));
  }

  @Test
  void append() throws Exception {
    MemcachedClient memcached = getMemcached(singletonMap("test-append", "append test"));
    testing.runWithSpan(
        "parent",
        () -> {
          CASValue<Object> casValue = memcached.gets(key("test-append"));
          assertThat(memcached.append(casValue.getCas(), key("test-append"), " appended").get())
              .isTrue();
          assertThat(memcached.get(key("test-append"))).isEqualTo("append test appended");
        });

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasNoParent().hasAttributes(Attributes.empty()),
                span ->
                    span.hasName("gets")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.MEMCACHED),
                            equalTo(maybeStable(DB_OPERATION), "gets")),
                span ->
                    span.hasName("append")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.MEMCACHED),
                            equalTo(maybeStable(DB_OPERATION), "append")),
                span ->
                    span.hasName("get")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.MEMCACHED),
                            equalTo(maybeStable(DB_OPERATION), "get"),
                            equalTo(stringKey("spymemcached.result"), "hit"))));
  }

  @Test
  void prepend() throws Exception {
    MemcachedClient memcached = getMemcached(singletonMap("test-prepend", "prepend test"));
    testing.runWithSpan(
        "parent",
        () -> {
          CASValue<Object> casValue = memcached.gets(key("test-prepend"));
          assertThat(memcached.prepend(casValue.getCas(), key("test-prepend"), "prepended ").get())
              .isTrue();
          assertThat(memcached.get(key("test-prepend"))).isEqualTo("prepended prepend test");
        });

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasNoParent().hasAttributes(Attributes.empty()),
                span ->
                    span.hasName("gets")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.MEMCACHED),
                            equalTo(maybeStable(DB_OPERATION), "gets")),
                span ->
                    span.hasName("prepend")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.MEMCACHED),
                            equalTo(maybeStable(DB_OPERATION), "prepend")),
                span ->
                    span.hasName("get")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.MEMCACHED),
                            equalTo(maybeStable(DB_OPERATION), "get"),
                            equalTo(stringKey("spymemcached.result"), "hit"))));
  }

  @Test
  void cas() {
    MemcachedClient memcached = getMemcached(singletonMap("test-cas", "cas test"));
    testing.runWithSpan(
        "parent",
        () -> {
          CASValue<Object> casValue = memcached.gets(key("test-cas"));
          assertThat(
                  memcached.cas(key("test-cas"), casValue.getCas(), EXPIRATION_SECONDS, "cas bar"))
              .isEqualTo(CASResponse.OK);
        });

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasNoParent().hasAttributes(Attributes.empty()),
                span ->
                    span.hasName("gets")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.MEMCACHED),
                            equalTo(maybeStable(DB_OPERATION), "gets")),
                span ->
                    span.hasName("cas")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.MEMCACHED),
                            equalTo(maybeStable(DB_OPERATION), "cas"))));
  }

  @Test
  void casNotFound() {
    MemcachedClient memcached = getMemcached();
    testing.runWithSpan(
        "parent",
        () -> {
          assertThat(
                  memcached.cas(key("test-cas-doesnt-exist"), 1234, EXPIRATION_SECONDS, "cas bar"))
              .isEqualTo(CASResponse.NOT_FOUND);
        });

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasNoParent().hasAttributes(Attributes.empty()),
                span ->
                    span.hasName("cas")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.MEMCACHED),
                            equalTo(maybeStable(DB_OPERATION), "cas"))));
  }

  @Test
  void touch() throws Exception {
    MemcachedClient memcached = getMemcached(singletonMap("test-touch", "touch test"));
    testing.runWithSpan(
        "parent",
        () -> {
          assertThat(memcached.touch(key("test-touch"), EXPIRATION_SECONDS).get()).isTrue();
        });

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasNoParent().hasAttributes(Attributes.empty()),
                span ->
                    span.hasName("touch")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.MEMCACHED),
                            equalTo(maybeStable(DB_OPERATION), "touch"))));
  }

  @Test
  void touchNonExistent() throws Exception {
    MemcachedClient memcached = getMemcached();
    testing.runWithSpan(
        "parent",
        () -> {
          assertThat(memcached.touch(key("test-touch-non-existent"), EXPIRATION_SECONDS).get())
              .isFalse();
        });

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasNoParent().hasAttributes(Attributes.empty()),
                span ->
                    span.hasName("touch")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.MEMCACHED),
                            equalTo(maybeStable(DB_OPERATION), "touch"))));
  }

  @Test
  void getAndTouch() {
    MemcachedClient memcached = getMemcached(singletonMap("test-touch", "touch test"));
    testing.runWithSpan(
        "parent",
        () -> {
          assertThat(memcached.getAndTouch(key("test-touch"), EXPIRATION_SECONDS).getValue())
              .isEqualTo("touch test");
        });

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasNoParent().hasAttributes(Attributes.empty()),
                span ->
                    span.hasName("getAndTouch")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.MEMCACHED),
                            equalTo(maybeStable(DB_OPERATION), "getAndTouch"))));
  }

  @Test
  void getAndTouchNonExistent() {
    MemcachedClient memcached = getMemcached();
    testing.runWithSpan(
        "parent",
        () -> {
          assertThat(memcached.getAndTouch(key("test-touch-non-existent"), EXPIRATION_SECONDS))
              .isNull();
        });

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasNoParent().hasAttributes(Attributes.empty()),
                span ->
                    span.hasName("getAndTouch")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.MEMCACHED),
                            equalTo(maybeStable(DB_OPERATION), "getAndTouch"))));
  }

  @Test
  void decr() {
    MemcachedClient memcached = getMemcached(singletonMap("test-decr", "200"));
    testing.runWithSpan(
        "parent",
        () -> {
          /*
           Memcached is funny in the way it handles incr/decr operations:
           it needs values to be strings (with digits in them) and it returns actual long from decr/incr
          */
          assertThat(memcached.decr(key("test-decr"), 5)).isEqualTo(195);
          assertThat(memcached.get(key("test-decr"))).isEqualTo("195");
        });

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasNoParent().hasAttributes(Attributes.empty()),
                span ->
                    span.hasName("decr")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.MEMCACHED),
                            equalTo(maybeStable(DB_OPERATION), "decr")),
                span ->
                    span.hasName("get")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.MEMCACHED),
                            equalTo(maybeStable(DB_OPERATION), "get"),
                            equalTo(stringKey("spymemcached.result"), "hit"))));
  }

  @Test
  void decrNonExistent() {
    MemcachedClient memcached = getMemcached();
    testing.runWithSpan(
        "parent",
        () -> {
          assertThat(memcached.decr(key("test-decr-non-existent"), 5)).isEqualTo(-1);
        });

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasNoParent().hasAttributes(Attributes.empty()),
                span ->
                    span.hasName("decr")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.MEMCACHED),
                            equalTo(maybeStable(DB_OPERATION), "decr"))));
  }

  @Test
  void decrException() {
    MemcachedClient memcached = getMemcached();
    assertThatThrownBy(() -> memcached.decr(key("long key: " + longString()), 5))
        .isInstanceOf(IllegalArgumentException.class);

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("decr")
                        .hasKind(SpanKind.CLIENT)
                        .hasNoParent()
                        .hasStatus(StatusData.error())
                        .hasException(
                            new IllegalArgumentException("Key is too long (maxlen = 250)"))
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.MEMCACHED),
                            equalTo(maybeStable(DB_OPERATION), "decr"),
                            equalTo(
                                ERROR_TYPE,
                                "net.spy.memcached.internal.CheckedOperationTimeoutException"))));
  }

  @Test
  void incr() {
    MemcachedClient memcached = getMemcached(singletonMap("test-incr", "100"));
    testing.runWithSpan(
        "parent",
        () -> {
          /*
           Memcached is funny in the way it handles incr/decr operations:
           it needs values to be strings (with digits in them) and it returns actual long from decr/incr
          */
          assertThat(memcached.incr(key("test-incr"), 5)).isEqualTo(105);
          assertThat(memcached.get(key("test-incr"))).isEqualTo("105");
        });

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasNoParent().hasAttributes(Attributes.empty()),
                span ->
                    span.hasName("incr")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.MEMCACHED),
                            equalTo(maybeStable(DB_OPERATION), "incr")),
                span ->
                    span.hasName("get")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.MEMCACHED),
                            equalTo(maybeStable(DB_OPERATION), "get"),
                            equalTo(stringKey("spymemcached.result"), "hit"))));
  }

  @Test
  void incrNonExistent() {
    MemcachedClient memcached = getMemcached();
    testing.runWithSpan(
        "parent",
        () -> {
          assertThat(memcached.incr(key("test-incr-non-existent"), 5)).isEqualTo(-1);
        });

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasNoParent().hasAttributes(Attributes.empty()),
                span ->
                    span.hasName("incr")
                        .hasKind(SpanKind.CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.MEMCACHED),
                            equalTo(maybeStable(DB_OPERATION), "incr"))));
  }

  @Test
  void incrException() {
    MemcachedClient memcached = getMemcached();
    assertThatThrownBy(() -> memcached.incr(key("long key: " + longString()), 5))
        .isInstanceOf(IllegalArgumentException.class);

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("incr")
                        .hasKind(SpanKind.CLIENT)
                        .hasNoParent()
                        .hasStatus(StatusData.error())
                        .hasException(
                            new IllegalArgumentException("Key is too long (maxlen = 250)"))
                        .hasAttributesSatisfyingExactly(
                            equalTo(
                                maybeStable(DB_SYSTEM),
                                DbIncubatingAttributes.DbSystemIncubatingValues.MEMCACHED),
                            equalTo(maybeStable(DB_OPERATION), "incr"))));
  }

  private static String key(String k) {
    return KEY_PREFIX + k;
  }

  private static String longString() {
    char[] chars = new char[250];
    Arrays.fill(chars, 's');
    return new String(chars);
  }

  private static BlockingQueue<Operation> getLockableQueue(ReentrantLock queueLock) {
    return new ArrayBlockingQueue<Operation>(DefaultConnectionFactory.DEFAULT_OP_QUEUE_LEN) {

      @Override
      public int drainTo(Collection<? super Operation> c, int maxElements) {
        queueLock.lock();
        try {
          return super.drainTo(c, maxElements);
        } finally {
          queueLock.unlock();
        }
      }
    };
  }
}
