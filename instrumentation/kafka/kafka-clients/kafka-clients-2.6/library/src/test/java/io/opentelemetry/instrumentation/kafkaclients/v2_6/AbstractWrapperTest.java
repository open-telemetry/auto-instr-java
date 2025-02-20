/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.kafkaclients.v2_6;

import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaClientBaseTest;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.LibraryInstrumentationExtension;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

abstract class AbstractWrapperTest extends KafkaClientBaseTest {

  @RegisterExtension
  static final InstrumentationExtension testing = LibraryInstrumentationExtension.create();

  static final String greeting = "Hello Kafka!";

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void testWrappers(boolean testHeaders) throws InterruptedException {
    KafkaTelemetryBuilder telemetryBuilder =
        KafkaTelemetry.builder(testing.getOpenTelemetry())
            .setCapturedHeaders(singletonList("test-message-header"))
            // TODO run tests both with and without experimental span attributes
            .setCaptureExperimentalSpanAttributes(true);
    configure(telemetryBuilder);
    KafkaTelemetry telemetry = telemetryBuilder.build();

    Producer<Integer, String> wrappedProducer = telemetry.wrap(producer);

    testing.runWithSpan(
        "parent",
        () -> {
          ProducerRecord<Integer, String> producerRecord =
              new ProducerRecord<>(SHARED_TOPIC, greeting);
          if (testHeaders) {
            producerRecord
                .headers()
                .add("test-message-header", "test".getBytes(StandardCharsets.UTF_8));
          }
          wrappedProducer.send(
              producerRecord,
              (meta, ex) -> {
                if (ex == null) {
                  testing.runWithSpan("producer callback", () -> {});
                } else {
                  testing.runWithSpan("producer exception: " + ex, () -> {});
                }
              });
        });

    awaitUntilConsumerIsReady();
    Consumer<Integer, String> wrappedConsumer = telemetry.wrap(consumer);
    ConsumerRecords<?, ?> records = wrappedConsumer.poll(Duration.ofSeconds(10));
    assertThat(records.count()).isEqualTo(1);
    for (ConsumerRecord<?, ?> record : records) {
      assertThat(record.value()).isEqualTo(greeting);
      assertThat(record.key()).isNull();
      testing.runWithSpan("process child", () -> {});
    }

    assertTraces(testHeaders);
  }

  abstract void configure(KafkaTelemetryBuilder builder);

  abstract void assertTraces(boolean testHeaders);
}
