/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.kafkaclients.v2_6;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaClientBaseTest;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.LibraryInstrumentationExtension;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

abstract class AbstractInterceptorsTest extends KafkaClientBaseTest {

  @RegisterExtension
  static final InstrumentationExtension testing = LibraryInstrumentationExtension.create();

  static final String greeting = "Hello Kafka!";

  @Override
  public Map<String, Object> producerProps() {
    Map<String, Object> props = super.producerProps();
    props.put(
        ProducerConfig.INTERCEPTOR_CLASSES_CONFIG, TracingProducerInterceptor.class.getName());
    return props;
  }

  @Override
  public Map<String, Object> consumerProps() {
    Map<String, Object> props = super.consumerProps();
    props.put(
        ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG, TracingConsumerInterceptor.class.getName());
    return props;
  }

  @Test
  void testInterceptors() throws InterruptedException {
    testing.runWithSpan(
        "parent",
        () -> {
          ProducerRecord<Integer, String> producerRecord =
              new ProducerRecord<>(SHARED_TOPIC, greeting);
          producerRecord
              .headers()
              // adding baggage header in w3c baggage format
              .add(
                  "baggage",
                  "test-baggage-key-1=test-baggage-value-1".getBytes(StandardCharsets.UTF_8))
              .add(
                  "baggage",
                  "test-baggage-key-2=test-baggage-value-2".getBytes(StandardCharsets.UTF_8));
          producer.send(
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
    // check that the message was received
    ConsumerRecords<?, ?> records = consumer.poll(Duration.ofSeconds(5));
    assertThat(records.count()).isEqualTo(1);
    for (ConsumerRecord<?, ?> record : records) {
      assertThat(record.value()).isEqualTo(greeting);
      assertThat(record.key()).isNull();
      testing.runWithSpan("process child", () -> {});
    }

    assertTraces();
  }

  abstract void assertTraces();
}
