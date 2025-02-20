/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.satisfies;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_BATCH_MESSAGE_COUNT;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_DESTINATION_NAME;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_DESTINATION_PARTITION_ID;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_KAFKA_CONSUMER_GROUP;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_KAFKA_MESSAGE_KEY;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_KAFKA_MESSAGE_OFFSET;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_KAFKA_MESSAGE_TOMBSTONE;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_MESSAGE_BODY_SIZE;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_OPERATION;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_SYSTEM;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.sdk.testing.assertj.AttributeAssertion;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRebalanceListener;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.IntegerDeserializer;
import org.apache.kafka.common.serialization.IntegerSerializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.assertj.core.api.AbstractLongAssert;
import org.assertj.core.api.AbstractStringAssert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

@SuppressWarnings("OtelInternalJavadoc")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class KafkaClientBaseTest {
  private static final Logger logger = LoggerFactory.getLogger(KafkaClientBaseTest.class);

  protected static final String SHARED_TOPIC = "shared.topic";
  protected static final AttributeKey<String> MESSAGING_CLIENT_ID =
      AttributeKey.stringKey("messaging.client_id");

  private KafkaContainer kafka;
  protected Producer<Integer, String> producer;
  protected Consumer<Integer, String> consumer;
  private final CountDownLatch consumerReady = new CountDownLatch(1);

  public static final int partition = 0;
  public static final TopicPartition topicPartition = new TopicPartition(SHARED_TOPIC, partition);

  @BeforeAll
  void setupClass() throws ExecutionException, InterruptedException, TimeoutException {
    kafka =
        new KafkaContainer(DockerImageName.parse("apache/kafka:3.8.0"))
            .withEnv("KAFKA_HEAP_OPTS", "-Xmx256m")
            .withLogConsumer(new Slf4jLogConsumer(logger))
            .waitingFor(Wait.forLogMessage(".*started \\(kafka.server.Kafka.*Server\\).*", 1))
            .withStartupTimeout(Duration.ofMinutes(1));
    kafka.start();

    // create test topic
    HashMap<String, Object> adminProps = new HashMap<>();
    adminProps.put("bootstrap.servers", kafka.getBootstrapServers());

    try (AdminClient admin = AdminClient.create(adminProps)) {
      admin
          .createTopics(Collections.singletonList(new NewTopic(SHARED_TOPIC, 1, (short) 1)))
          .all()
          .get(30, TimeUnit.SECONDS);
    }

    producer = new KafkaProducer<>(producerProps());

    consumer = new KafkaConsumer<>(consumerProps());

    consumer.subscribe(
        Collections.singletonList(SHARED_TOPIC),
        new ConsumerRebalanceListener() {
          @Override
          public void onPartitionsRevoked(Collection<TopicPartition> collection) {}

          @Override
          public void onPartitionsAssigned(Collection<TopicPartition> collection) {
            consumerReady.countDown();
          }
        });
  }

  public Map<String, Object> consumerProps() {
    HashMap<String, Object> props = new HashMap<>();
    props.put("bootstrap.servers", kafka.getBootstrapServers());
    props.put("group.id", "test");
    props.put("enable.auto.commit", "true");
    props.put("auto.commit.interval.ms", 10);
    props.put("session.timeout.ms", "30000");
    props.put("key.deserializer", IntegerDeserializer.class.getName());
    props.put("value.deserializer", StringDeserializer.class.getName());
    return props;
  }

  public Map<String, Object> producerProps() {
    HashMap<String, Object> props = new HashMap<>();
    props.put("bootstrap.servers", kafka.getBootstrapServers());
    props.put("retries", 0);
    props.put("batch.size", "16384");
    props.put("linger.ms", 1);
    props.put("buffer.memory", "33554432");
    props.put("key.serializer", IntegerSerializer.class.getName());
    props.put("value.serializer", StringSerializer.class.getName());
    return props;
  }

  @AfterAll
  void cleanupClass() {
    if (producer != null) {
      producer.close();
    }
    if (consumer != null) {
      consumer.close();
    }
    kafka.stop();
  }

  @SuppressWarnings("PreferJavaTimeOverload")
  public void awaitUntilConsumerIsReady() throws InterruptedException {
    if (consumerReady.await(0, TimeUnit.SECONDS)) {
      return;
    }
    for (int i = 0; i < 10; i++) {
      consumer.poll(0);
      if (consumerReady.await(1, TimeUnit.SECONDS)) {
        break;
      }
    }
    if (consumerReady.getCount() != 0) {
      throw new AssertionError("Consumer wasn't assigned any partitions!");
    }
    consumer.seekToBeginning(Collections.emptyList());
  }

  @SuppressWarnings("deprecation") // using deprecated semconv
  protected static List<AttributeAssertion> sendAttributes(
      String messageKey, String messageValue, boolean testHeaders) {
    List<AttributeAssertion> assertions =
        new ArrayList<>(
            Arrays.asList(
                equalTo(MESSAGING_SYSTEM, "kafka"),
                equalTo(MESSAGING_DESTINATION_NAME, SHARED_TOPIC),
                equalTo(MESSAGING_OPERATION, "publish"),
                satisfies(MESSAGING_CLIENT_ID, stringAssert -> stringAssert.startsWith("producer")),
                satisfies(MESSAGING_DESTINATION_PARTITION_ID, AbstractStringAssert::isNotEmpty),
                satisfies(MESSAGING_KAFKA_MESSAGE_OFFSET, AbstractLongAssert::isNotNegative)));
    if (messageKey != null) {
      assertions.add(equalTo(MESSAGING_KAFKA_MESSAGE_KEY, messageKey));
    }
    if (messageValue == null) {
      assertions.add(equalTo(MESSAGING_KAFKA_MESSAGE_TOMBSTONE, true));
    }
    if (testHeaders) {
      assertions.add(
          equalTo(
              AttributeKey.stringArrayKey("messaging.header.test_message_header"),
              Collections.singletonList("test")));
    }
    return assertions;
  }

  @SuppressWarnings("deprecation") // using deprecated semconv
  protected static List<AttributeAssertion> receiveAttributes(boolean testHeaders) {
    List<AttributeAssertion> assertions =
        new ArrayList<>(
            Arrays.asList(
                equalTo(MESSAGING_SYSTEM, "kafka"),
                equalTo(MESSAGING_DESTINATION_NAME, SHARED_TOPIC),
                equalTo(MESSAGING_OPERATION, "receive"),
                satisfies(MESSAGING_CLIENT_ID, stringAssert -> stringAssert.startsWith("consumer")),
                satisfies(MESSAGING_BATCH_MESSAGE_COUNT, AbstractLongAssert::isPositive)));
    // consumer group is not available in version 0.11
    if (Boolean.getBoolean("testLatestDeps")) {
      assertions.add(equalTo(MESSAGING_KAFKA_CONSUMER_GROUP, "test"));
    }
    if (testHeaders) {
      assertions.add(
          equalTo(
              AttributeKey.stringArrayKey("messaging.header.test_message_header"),
              Collections.singletonList("test")));
    }
    return assertions;
  }

  @SuppressWarnings("deprecation") // using deprecated semconv
  protected static List<AttributeAssertion> processAttributes(
      String messageKey, String messageValue, boolean testHeaders, boolean testMultiBaggage) {
    List<AttributeAssertion> assertions =
        new ArrayList<>(
            Arrays.asList(
                equalTo(MESSAGING_SYSTEM, "kafka"),
                equalTo(MESSAGING_DESTINATION_NAME, SHARED_TOPIC),
                equalTo(MESSAGING_OPERATION, "process"),
                satisfies(MESSAGING_CLIENT_ID, stringAssert -> stringAssert.startsWith("consumer")),
                satisfies(MESSAGING_DESTINATION_PARTITION_ID, AbstractStringAssert::isNotEmpty),
                satisfies(MESSAGING_KAFKA_MESSAGE_OFFSET, AbstractLongAssert::isNotNegative),
                satisfies(
                    AttributeKey.longKey("kafka.record.queue_time_ms"),
                    AbstractLongAssert::isNotNegative)));
    // consumer group is not available in version 0.11
    if (Boolean.getBoolean("testLatestDeps")) {
      assertions.add(equalTo(MESSAGING_KAFKA_CONSUMER_GROUP, "test"));
    }
    if (messageKey != null) {
      assertions.add(equalTo(MESSAGING_KAFKA_MESSAGE_KEY, messageKey));
    }
    if (messageValue == null) {
      assertions.add(equalTo(MESSAGING_KAFKA_MESSAGE_TOMBSTONE, true));
    } else {
      assertions.add(
          equalTo(
              MESSAGING_MESSAGE_BODY_SIZE, messageValue.getBytes(StandardCharsets.UTF_8).length));
    }
    if (testHeaders) {
      assertions.add(
          equalTo(
              AttributeKey.stringArrayKey("messaging.header.test_message_header"),
              Collections.singletonList("test")));
    }

    if (testMultiBaggage) {
      assertions.add(equalTo(AttributeKey.stringKey("test-baggage-key-1"), "test-baggage-value-1"));
      assertions.add(equalTo(AttributeKey.stringKey("test-baggage-key-2"), "test-baggage-value-2"));
    }
    return assertions;
  }
}
