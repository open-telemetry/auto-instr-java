/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.kafkaclients.v0_11;

import static io.opentelemetry.instrumentation.testing.util.TelemetryDataUtil.orderByRootSpanKind;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaClientBaseTest;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaClientPropagationBaseTest;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class KafkaClientDefaultTest extends KafkaClientPropagationBaseTest {

  @RegisterExtension
  static final InstrumentationExtension testing = AgentInstrumentationExtension.create();

  @DisplayName("test kafka produce and consume")
  @ParameterizedTest(name = "{index} => test headers: {0}")
  @ValueSource(booleans = {true, false})
  void testKafkaProducerAndConsumerSpan(boolean testHeaders) throws Exception {
    String greeting = "Hello Kafka!";
    testing.runWithSpan(
        "parent",
        () -> {
          ProducerRecord<Integer, String> producerRecord =
              new ProducerRecord<>(SHARED_TOPIC, 10, greeting);
          if (testHeaders) {
            producerRecord
                .headers()
                .add("test-message-header", "test".getBytes(StandardCharsets.UTF_8));
          }
          producer
              .send(
                  producerRecord,
                  (meta, ex) -> {
                    if (ex == null) {
                      testing.runWithSpan("producer callback", () -> {});
                    } else {
                      testing.runWithSpan("producer exception: " + ex, () -> {});
                    }
                  })
              .get(5, TimeUnit.SECONDS);
        });

    awaitUntilConsumerIsReady();
    @SuppressWarnings("PreferJavaTimeOverload")
    ConsumerRecords<?, ?> records = consumer.poll(Duration.ofSeconds(5).toMillis());
    assertThat(records.count()).isEqualTo(1);

    // iterate over records to generate spans
    for (ConsumerRecord<?, ?> record : records) {
      testing.runWithSpan(
          "processing",
          () -> {
            assertThat(record.key()).isEqualTo(10);
            assertThat(record.value()).isEqualTo(greeting);
          });
    }
    AtomicReference<SpanData> producerSpan = new AtomicReference<>();
    testing.waitAndAssertSortedTraces(
        orderByRootSpanKind(SpanKind.INTERNAL, SpanKind.CONSUMER),
        trace -> {
          trace.hasSpansSatisfyingExactly(
              span -> span.hasName("parent").hasKind(SpanKind.INTERNAL).hasNoParent(),
              span ->
                  span.hasName(SHARED_TOPIC + " publish")
                      .hasKind(SpanKind.PRODUCER)
                      .hasParent(trace.getSpan(0))
                      .hasAttributesSatisfyingExactly(sendAttributes("10", greeting, testHeaders)),
              span ->
                  span.hasName("producer callback")
                      .hasKind(SpanKind.INTERNAL)
                      .hasParent(trace.getSpan(0)));
          producerSpan.set(trace.getSpan(1));
        },
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName(SHARED_TOPIC + " receive")
                        .hasKind(SpanKind.CONSUMER)
                        .hasNoParent()
                        .hasAttributesSatisfyingExactly(receiveAttributes(testHeaders)),
                span ->
                    span.hasName(SHARED_TOPIC + " process")
                        .hasKind(SpanKind.CONSUMER)
                        .hasLinks(LinkData.create(producerSpan.get().getSpanContext()))
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            processAttributes("10", greeting, testHeaders, false)),
                span -> span.hasName("processing").hasParent(trace.getSpan(1))));
  }

  @DisplayName("test pass through tombstone")
  @Test
  void testPassThroughTombstone()
      throws ExecutionException, InterruptedException, TimeoutException {
    producer.send(new ProducerRecord<>(SHARED_TOPIC, null)).get(5, TimeUnit.SECONDS);
    awaitUntilConsumerIsReady();
    @SuppressWarnings("PreferJavaTimeOverload")
    ConsumerRecords<?, ?> records = consumer.poll(Duration.ofSeconds(5).toMillis());
    assertThat(records.count()).isEqualTo(1);

    // iterate over records to generate spans
    for (ConsumerRecord<?, ?> record : records) {
      assertThat(record.value()).isNull();
      assertThat(record.key()).isNull();
    }

    AtomicReference<SpanData> producerSpan = new AtomicReference<>();
    testing.waitAndAssertSortedTraces(
        orderByRootSpanKind(SpanKind.INTERNAL, SpanKind.CONSUMER),
        trace -> {
          trace.hasSpansSatisfyingExactly(
              span ->
                  span.hasName(SHARED_TOPIC + " publish")
                      .hasKind(SpanKind.PRODUCER)
                      .hasNoParent()
                      .hasAttributesSatisfyingExactly(sendAttributes(null, null, false)));
          producerSpan.set(trace.getSpan(0));
        },
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName(SHARED_TOPIC + " receive")
                        .hasKind(SpanKind.CONSUMER)
                        .hasNoParent()
                        .hasAttributesSatisfyingExactly(receiveAttributes(false)),
                span ->
                    span.hasName(SHARED_TOPIC + " process")
                        .hasKind(SpanKind.CONSUMER)
                        .hasLinks(LinkData.create(producerSpan.get().getSpanContext()))
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            processAttributes(null, null, false, false))));
  }

  @DisplayName("test records(TopicPartition) kafka consume")
  @Test
  void testRecordsWithTopicPartitionKafkaConsume()
      throws ExecutionException, InterruptedException, TimeoutException {
    String greeting = "Hello from MockConsumer!";
    producer
        .send(new ProducerRecord<>(SHARED_TOPIC, partition, null, greeting))
        .get(5, TimeUnit.SECONDS);

    testing.waitForTraces(1);

    awaitUntilConsumerIsReady();
    @SuppressWarnings("PreferJavaTimeOverload")
    ConsumerRecords<?, ?> consumerRecords = consumer.poll(Duration.ofSeconds(5).toMillis());
    List<? extends ConsumerRecord<?, ?>> recordsInPartition =
        consumerRecords.records(KafkaClientBaseTest.topicPartition);
    assertThat(recordsInPartition.size()).isEqualTo(1);

    // iterate over records to generate spans
    for (ConsumerRecord<?, ?> record : recordsInPartition) {
      assertThat(record.value()).isEqualTo(greeting);
      assertThat(record.key()).isNull();
    }

    AtomicReference<SpanData> producerSpan = new AtomicReference<>();
    testing.waitAndAssertSortedTraces(
        orderByRootSpanKind(SpanKind.INTERNAL, SpanKind.CONSUMER),
        trace -> {
          trace.hasSpansSatisfyingExactly(
              span ->
                  span.hasName(SHARED_TOPIC + " publish")
                      .hasKind(SpanKind.PRODUCER)
                      .hasNoParent()
                      .hasAttributesSatisfyingExactly(sendAttributes(null, greeting, false)));
          producerSpan.set(trace.getSpan(0));
        },
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName(SHARED_TOPIC + " receive")
                        .hasKind(SpanKind.CONSUMER)
                        .hasNoParent()
                        .hasAttributesSatisfyingExactly(receiveAttributes(false)),
                span ->
                    span.hasName(SHARED_TOPIC + " process")
                        .hasKind(SpanKind.CONSUMER)
                        .hasLinks(LinkData.create(producerSpan.get().getSpanContext()))
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            processAttributes(null, greeting, false, false))));
  }
}
