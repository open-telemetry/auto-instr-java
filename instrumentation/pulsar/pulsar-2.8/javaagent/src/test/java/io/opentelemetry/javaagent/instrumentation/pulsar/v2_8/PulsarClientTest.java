/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.pulsar.v2_8;

import static io.opentelemetry.instrumentation.testing.util.TelemetryDataUtil.orderByRootSpanName;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.satisfies;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.javaagent.instrumentation.pulsar.v2_8.telemetry.PulsarSingletons;
import io.opentelemetry.sdk.testing.assertj.AttributeAssertion;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.semconv.SemanticAttributes;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import org.apache.pulsar.client.admin.PulsarAdmin;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.MessageListener;
import org.apache.pulsar.client.api.Messages;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.Schema;
import org.apache.pulsar.client.api.SubscriptionInitialPosition;
import org.apache.pulsar.client.api.SubscriptionType;
import org.assertj.core.api.AbstractLongAssert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PulsarContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.utility.DockerImageName;

class PulsarClientTest {

  private static final Logger logger = LoggerFactory.getLogger(PulsarClientTest.class);

  private static final DockerImageName DEFAULT_IMAGE_NAME =
      DockerImageName.parse("apachepulsar/pulsar:2.8.0");

  @RegisterExtension
  static final InstrumentationExtension testing = AgentInstrumentationExtension.create();

  private static PulsarContainer pulsar;
  private static PulsarClient client;
  private static PulsarAdmin admin;
  private static String brokerHost;
  private static int brokerPort;

  private static final AttributeKey<String> MESSAGE_TYPE =
      AttributeKey.stringKey("messaging.pulsar.message.type");

  @BeforeAll
  static void beforeAll() throws PulsarClientException {
    pulsar =
        new PulsarContainer(DEFAULT_IMAGE_NAME)
            .withEnv("PULSAR_MEM", "-Xmx128m")
            .withLogConsumer(new Slf4jLogConsumer(logger))
            .withStartupTimeout(Duration.ofMinutes(2));
    pulsar.start();

    brokerHost = pulsar.getHost();
    brokerPort = pulsar.getMappedPort(6650);
    client =
        PulsarClient.builder()
            .serviceUrl(pulsar.getPulsarBrokerUrl())
            .statsInterval(5, TimeUnit.SECONDS)
            .build();
    admin = PulsarAdmin.builder().serviceHttpUrl(pulsar.getHttpServiceUrl()).build();
  }

  @AfterAll
  static void afterAll() throws PulsarClientException {
    if (client != null) {
      client.close();
    }
    if (admin != null) {
      admin.close();
    }
    pulsar.close();
  }

  @Test
  void testSendNonPartitionedTopic() throws Exception {
    String topic = "persistent://public/default/testSendNonPartitionedTopic";
    admin.topics().createNonPartitionedTopic(topic);
    Producer<String> producer =
        client.newProducer(Schema.STRING).topic(topic).enableBatching(false).create();

    String msg = "test";
    MessageId msgId = testing.runWithSpan("parent", () -> producer.send(msg));

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasKind(SpanKind.INTERNAL).hasNoParent(),
                span ->
                    span.hasName(topic + " publish")
                        .hasKind(SpanKind.PRODUCER)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            sendAttributes(topic, msgId.toString(), false))));
    producer.close();
  }

  @Test
  void testConsumeNonPartitionedTopic() throws Exception {
    String topic = "persistent://public/default/testConsumeNonPartitionedTopic";
    CountDownLatch latch = new CountDownLatch(1);
    admin.topics().createNonPartitionedTopic(topic);
    Consumer<String> c =
        client
            .newConsumer(Schema.STRING)
            .subscriptionName("test_sub")
            .topic(topic)
            .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
            .messageListener(
                (MessageListener<String>)
                    (consumer, msg) -> {
                      acknowledgeMessage(consumer, msg);
                      latch.countDown();
                    })
            .subscribe();

    Producer<String> producer =
        client.newProducer(Schema.STRING).topic(topic).enableBatching(false).create();

    String msg = "test";
    MessageId msgId = testing.runWithSpan("parent", () -> producer.send(msg));

    latch.await(1, TimeUnit.MINUTES);

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasKind(SpanKind.INTERNAL).hasNoParent(),
                span ->
                    span.hasName(topic + " publish")
                        .hasKind(SpanKind.PRODUCER)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            sendAttributes(topic, msgId.toString(), false)),
                span ->
                    span.hasName(topic + " receive")
                        .hasKind(SpanKind.CONSUMER)
                        .hasParent(trace.getSpan(1))
                        .hasAttributesSatisfyingExactly(
                            receiveAttributes(topic, msgId.toString(), false)),
                span ->
                    span.hasName(topic + " process")
                        .hasKind(SpanKind.INTERNAL)
                        .hasParent(trace.getSpan(2))
                        .hasAttributesSatisfyingExactly(
                            processAttributes(topic, msgId.toString(), false))));
    c.close();
    producer.close();
  }

  @Test
  void testConsumeNonPartitionedTopicUsingReceive() throws Exception {
    String topic = "persistent://public/default/testConsumeNonPartitionedTopicCallReceive";
    admin.topics().createNonPartitionedTopic(topic);
    Consumer<String> consumer =
        client
            .newConsumer(Schema.STRING)
            .subscriptionName("test_sub")
            .topic(topic)
            .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
            .subscribe();
    Producer<String> producer =
        client.newProducer(Schema.STRING).topic(topic).enableBatching(false).create();

    String msg = "test";
    MessageId msgId = testing.runWithSpan("parent", () -> producer.send(msg));

    Message<String> receivedMsg = consumer.receive();
    consumer.acknowledge(receivedMsg);

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasKind(SpanKind.INTERNAL).hasNoParent(),
                span ->
                    span.hasName(topic + " publish")
                        .hasKind(SpanKind.PRODUCER)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            sendAttributes(topic, msgId.toString(), false)),
                span ->
                    span.hasName(topic + " receive")
                        .hasKind(SpanKind.CONSUMER)
                        .hasParent(trace.getSpan(1))
                        .hasAttributesSatisfyingExactly(
                            receiveAttributes(topic, msgId.toString(), false))));
    consumer.close();
    producer.close();
  }

  @Test
  void testConsumeNonPartitionedTopicUsingReceiveAsync() throws Exception {
    String topic = "persistent://public/default/testConsumeNonPartitionedTopicCallReceiveAsync";
    admin.topics().createNonPartitionedTopic(topic);
    Consumer<String> consumer =
        client
            .newConsumer(Schema.STRING)
            .subscriptionName("test_sub")
            .topic(topic)
            .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
            .subscribe();

    Producer<String> producer =
        client.newProducer(Schema.STRING).topic(topic).enableBatching(false).create();

    CompletableFuture<Message<String>> result =
        consumer
            .receiveAsync()
            .whenComplete(
                (message, throwable) -> {
                  if (message != null) {
                    testing.runWithSpan("callback", () -> acknowledgeMessage(consumer, message));
                  }
                });

    String msg = "test";
    MessageId msgId = testing.runWithSpan("parent", () -> producer.send(msg));

    result.get(1, TimeUnit.MINUTES);

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasKind(SpanKind.INTERNAL).hasNoParent(),
                span ->
                    span.hasName(topic + " publish")
                        .hasKind(SpanKind.PRODUCER)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            sendAttributes(topic, msgId.toString(), false)),
                span ->
                    span.hasName(topic + " receive")
                        .hasKind(SpanKind.CONSUMER)
                        .hasParent(trace.getSpan(1))
                        .hasAttributesSatisfyingExactly(
                            receiveAttributes(topic, msgId.toString(), false)),
                span ->
                    span.hasName("callback")
                        .hasKind(SpanKind.INTERNAL)
                        .hasParent(trace.getSpan(2))));
    consumer.close();
    producer.close();
  }

  @Test
  void testConsumeNonPartitionedTopicUsingReceiveWithTimeout() throws Exception {
    String topic =
        "persistent://public/default/testConsumeNonPartitionedTopicCallReceiveWithTimeout";
    admin.topics().createNonPartitionedTopic(topic);
    Consumer<String> consumer =
        client
            .newConsumer(Schema.STRING)
            .subscriptionName("test_sub")
            .topic(topic)
            .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
            .subscribe();
    Producer<String> producer =
        client.newProducer(Schema.STRING).topic(topic).enableBatching(false).create();

    String msg = "test";
    MessageId msgId = testing.runWithSpan("parent", () -> producer.send(msg));

    Message<String> receivedMsg = consumer.receive(1, TimeUnit.MINUTES);
    consumer.acknowledge(receivedMsg);

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasKind(SpanKind.INTERNAL).hasNoParent(),
                span ->
                    span.hasName(topic + " publish")
                        .hasKind(SpanKind.PRODUCER)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            sendAttributes(topic, msgId.toString(), false)),
                span ->
                    span.hasName(topic + " receive")
                        .hasKind(SpanKind.CONSUMER)
                        .hasParent(trace.getSpan(1))
                        .hasAttributesSatisfyingExactly(
                            receiveAttributes(topic, msgId.toString(), false))));
    consumer.close();
    producer.close();
  }

  @Test
  void testConsumeNonPartitionedTopicUsingBatchReceive() throws Exception {
    String topic = "persistent://public/default/testConsumeNonPartitionedTopicCallBatchReceive";
    admin.topics().createNonPartitionedTopic(topic);
    Consumer<String> consumer =
        client
            .newConsumer(Schema.STRING)
            .subscriptionName("test_sub")
            .topic(topic)
            .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
            .subscribe();

    Producer<String> producer =
        client.newProducer(Schema.STRING).topic(topic).enableBatching(false).create();

    String msg = "test";
    MessageId msgId = testing.runWithSpan("parent", () -> producer.send(msg));

    testing.runWithSpan(
        "receive-parent",
        () -> {
          Messages<String> receivedMsg = consumer.batchReceive();
          consumer.acknowledge(receivedMsg);
        });
    AtomicReference<SpanData> producerSpan = new AtomicReference<>();

    testing.waitAndAssertTraces(
        trace -> {
          trace.hasSpansSatisfyingExactly(
              span -> span.hasName("parent").hasKind(SpanKind.INTERNAL).hasNoParent(),
              span ->
                  span.hasName(topic + " publish")
                      .hasKind(SpanKind.PRODUCER)
                      .hasParent(trace.getSpan(0))
                      .hasAttributesSatisfyingExactly(
                          sendAttributes(topic, msgId.toString(), false)));
          producerSpan.set(trace.getSpan(1));
        },
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("receive-parent").hasKind(SpanKind.INTERNAL).hasNoParent(),
                span ->
                    span.hasName(topic + " receive")
                        .hasKind(SpanKind.CONSUMER)
                        .hasLinks(LinkData.create(producerSpan.get().getSpanContext()))
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            batchReceiveAttributes(topic, null, false))));
    consumer.close();
    producer.close();
  }

  @Test
  void testConsumeNonPartitionedTopicUsingBatchReceiveAsync() throws Exception {
    String topic =
        "persistent://public/default/testConsumeNonPartitionedTopicCallBatchReceiveAsync";
    admin.topics().createNonPartitionedTopic(topic);
    Consumer<String> consumer =
        client
            .newConsumer(Schema.STRING)
            .subscriptionName("test_sub")
            .topic(topic)
            .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
            .subscribe();

    Producer<String> producer =
        client.newProducer(Schema.STRING).topic(topic).enableBatching(false).create();

    String msg = "test";
    MessageId msgId = testing.runWithSpan("parent", () -> producer.send(msg));

    CompletableFuture<Messages<String>> result =
        testing.runWithSpan(
            "receive-parent",
            () ->
                consumer
                    .batchReceiveAsync()
                    .whenComplete(
                        (messages, throwable) -> {
                          if (messages != null) {
                            testing.runWithSpan(
                                "callback", () -> acknowledgeMessages(consumer, messages));
                          }
                        }));

    assertThat(result.get(1, TimeUnit.MINUTES).size()).isEqualTo(1);

    AtomicReference<SpanData> producerSpan = new AtomicReference<>();
    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasKind(SpanKind.INTERNAL).hasNoParent(),
                span -> {
                  span.hasName(topic + " publish")
                      .hasKind(SpanKind.PRODUCER)
                      .hasParent(trace.getSpan(0))
                      .hasAttributesSatisfyingExactly(
                          sendAttributes(topic, msgId.toString(), false));
                  producerSpan.set(trace.getSpan(1));
                }),
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("receive-parent").hasKind(SpanKind.INTERNAL).hasNoParent(),
                span ->
                    span.hasName(topic + " receive")
                        .hasKind(SpanKind.CONSUMER)
                        .hasParent(trace.getSpan(0))
                        .hasLinks(LinkData.create(producerSpan.get().getSpanContext()))
                        .hasAttributesSatisfyingExactly(batchReceiveAttributes(topic, null, false)),
                span ->
                    span.hasName("callback")
                        .hasKind(SpanKind.INTERNAL)
                        .hasParent(trace.getSpan(1))));
    consumer.close();
    producer.close();
  }

  @Test
  void captureMessageHeaderAsSpanAttribute() throws Exception {
    String topic = "persistent://public/default/testCaptureMessageHeaderTopic";
    CountDownLatch latch = new CountDownLatch(1);
    admin.topics().createNonPartitionedTopic(topic);
    Consumer<String> c =
        client
            .newConsumer(Schema.STRING)
            .subscriptionName("test_sub")
            .topic(topic)
            .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
            .messageListener(
                (MessageListener<String>)
                    (consumer, msg) -> {
                      acknowledgeMessage(consumer, msg);
                      latch.countDown();
                    })
            .subscribe();

    Producer<String> producer =
        client.newProducer(Schema.STRING).topic(topic).enableBatching(false).create();

    String msg = "test";
    MessageId msgId =
        testing.runWithSpan(
            "parent",
            () -> producer.newMessage().value(msg).property("test-message-header", "test").send());

    latch.await(1, TimeUnit.MINUTES);

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasKind(SpanKind.INTERNAL).hasNoParent(),
                span ->
                    span.hasName(topic + " publish")
                        .hasKind(SpanKind.PRODUCER)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            sendAttributes(topic, msgId.toString(), true)),
                span ->
                    span.hasName(topic + " receive")
                        .hasKind(SpanKind.CONSUMER)
                        .hasParent(trace.getSpan(1))
                        .hasAttributesSatisfyingExactly(
                            receiveAttributes(topic, msgId.toString(), true)),
                span ->
                    span.hasName(topic + " process")
                        .hasKind(SpanKind.INTERNAL)
                        .hasParent(trace.getSpan(2))
                        .hasAttributesSatisfyingExactly(
                            processAttributes(topic, msgId.toString(), true))));
    c.close();
    producer.close();
  }

  @Test
  void testSendPartitionedTopic() throws Exception {
    String topic = "persistent://public/default/testSendPartitionedTopic";
    admin.topics().createPartitionedTopic(topic, 1);
    Producer<String> producer =
        client.newProducer(Schema.STRING).topic(topic).enableBatching(false).create();

    String msg = "test";
    MessageId msgId = testing.runWithSpan("parent", () -> producer.send(msg));

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasKind(SpanKind.INTERNAL).hasNoParent(),
                span ->
                    span.hasName(topic + "-partition-0 publish")
                        .hasKind(SpanKind.PRODUCER)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            sendAttributes(topic + "-partition-0", msgId.toString(), false))));
    producer.close();
  }

  @Test
  void testConsumePartitionedTopic() throws Exception {
    String topic = "persistent://public/default/testConsumePartitionedTopic";
    admin.topics().createPartitionedTopic(topic, 1);
    CountDownLatch latch = new CountDownLatch(1);
    Consumer<String> c =
        client
            .newConsumer(Schema.STRING)
            .subscriptionName("test_sub")
            .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
            .topic(topic)
            .messageListener(
                (MessageListener<String>)
                    (consumer, msg) -> {
                      acknowledgeMessage(consumer, msg);
                      latch.countDown();
                    })
            .subscribe();

    Producer<String> producer =
        client.newProducer(Schema.STRING).topic(topic).enableBatching(false).create();

    String msg = "test";
    MessageId msgId = testing.runWithSpan("parent", () -> producer.send(msg));

    latch.await(1, TimeUnit.MINUTES);

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent").hasKind(SpanKind.INTERNAL).hasNoParent(),
                span ->
                    span.hasName(topic + "-partition-0 publish")
                        .hasKind(SpanKind.PRODUCER)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            sendAttributes(topic + "-partition-0", msgId.toString(), false)),
                span ->
                    span.hasName(topic + "-partition-0 receive")
                        .hasKind(SpanKind.CONSUMER)
                        .hasParent(trace.getSpan(1))
                        .hasAttributesSatisfyingExactly(
                            receiveAttributes(topic + "-partition-0", msgId.toString(), false)),
                span ->
                    span.hasName(topic + "-partition-0 process")
                        .hasKind(SpanKind.INTERNAL)
                        .hasParent(trace.getSpan(2))
                        .hasAttributesSatisfyingExactly(
                            processAttributes(topic + "-partition-0", msgId.toString(), false))));
    c.close();
    producer.close();
  }

  @Test
  void testConsumeMultiTopics() throws Exception {
    String topicNamePrefix = "persistent://public/default/testConsumeMulti_";
    String topic1 = topicNamePrefix + "1";
    String topic2 = topicNamePrefix + "2";
    CountDownLatch latch = new CountDownLatch(2);
    Producer<String> producer =
        client.newProducer(Schema.STRING).topic(topic1).enableBatching(false).create();
    Producer<String> producer2 =
        client.newProducer(Schema.STRING).topic(topic2).enableBatching(false).create();

    MessageId msgId1 = testing.runWithSpan("parent1", () -> producer.send("test1"));
    MessageId msgId2 = testing.runWithSpan("parent2", () -> producer2.send("test2"));
    Consumer<String> c =
        client
            .newConsumer(Schema.STRING)
            .topic(topic2, topic1)
            .subscriptionName("test_sub")
            .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
            .messageListener(
                (MessageListener<String>)
                    (consumer, msg) -> {
                      acknowledgeMessage(consumer, msg);
                      latch.countDown();
                    })
            .subscribe();

    latch.await(1, TimeUnit.MINUTES);

    testing.waitAndAssertSortedTraces(
        orderByRootSpanName("parent1", "parent2"),
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent1").hasKind(SpanKind.INTERNAL).hasNoParent(),
                span ->
                    span.hasName(topic1 + " publish")
                        .hasKind(SpanKind.PRODUCER)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            sendAttributes(topic1, msgId1.toString(), false)),
                span ->
                    span.hasName(topic1 + " receive")
                        .hasKind(SpanKind.CONSUMER)
                        .hasParent(trace.getSpan(1))
                        .hasAttributesSatisfyingExactly(
                            receiveAttributes(topic1, msgId1.toString(), false)),
                span ->
                    span.hasName(topic1 + " process")
                        .hasKind(SpanKind.INTERNAL)
                        .hasParent(trace.getSpan(2))
                        .hasAttributesSatisfyingExactly(
                            processAttributes(topic1, msgId1.toString(), false))),
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("parent2").hasKind(SpanKind.INTERNAL).hasNoParent(),
                span ->
                    span.hasName(topic2 + " publish")
                        .hasKind(SpanKind.PRODUCER)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            sendAttributes(topic2, msgId2.toString(), false)),
                span ->
                    span.hasName(topic2 + " receive")
                        .hasKind(SpanKind.CONSUMER)
                        .hasParent(trace.getSpan(1))
                        .hasAttributesSatisfyingExactly(
                            receiveAttributes(topic2, msgId2.toString(), false)),
                span ->
                    span.hasName(topic2 + " process")
                        .hasKind(SpanKind.INTERNAL)
                        .hasParent(trace.getSpan(2))
                        .hasAttributesSatisfyingExactly(
                            processAttributes(topic2, msgId2.toString(), false))));
    c.close();
    producer.close();
    producer2.close();
  }

  @Test
  void testClientMetrics() throws Exception {
    String topic = "persistent://public/default/testMetrics";
    int producerCount = 2;
    int consumerCount = 2;
    List<Producer<String>> producers = new ArrayList<>(producerCount);
    for (int i = 0; i < producerCount; i++) {
      producers.add(
          client.newProducer(Schema.STRING).producerName("producer-" + i).topic(topic).create());
    }

    MessageListener<String> listener = Consumer::acknowledgeAsync;
    List<Consumer<String>> consumers = new ArrayList<>(consumerCount);
    for (int i = 0; i < 2; i++) {
      consumers.add(
          client
              .newConsumer(Schema.STRING)
              .topic(topic)
              .subscriptionName("sub")
              .consumerName("consumer-" + i)
              .subscriptionType(SubscriptionType.Shared)
              .messageListener(listener)
              .subscribe());
    }

    int messages = 100;
    for (Producer<String> producer : producers) {
      for (int i = 0; i < messages; i++) {
        producer.sendAsync("test_" + i);
      }
    }

    testing.waitAndAssertMetrics(
        PulsarSingletons.INSTRUMENTATION_NAME,
        "pulsarclient.producer.message.send.size",
        metrics ->
            metrics.anySatisfy(
                metric ->
                    assertThat(metric)
                        .hasDescription("Counts the size of sent messages")
                        .hasUnit("By")
                        .hasLongGaugeSatisfying(
                            __ ->
                                assertThat(metric.getLongGaugeData().getPoints())
                                    .anySatisfy(
                                        point -> {
                                          assertThat(point.getValue()).isPositive();
                                          assertThat(point.getAttributes())
                                              .containsEntry("pulsar.producer.name", "producer-0");
                                        }))));

    testing.waitAndAssertMetrics(
        PulsarSingletons.INSTRUMENTATION_NAME,
        "pulsarclient.producer.message.send.count",
        metrics ->
            metrics.anySatisfy(
                metric ->
                    assertThat(metric)
                        .hasDescription("Counts the number of sent messages")
                        .hasUnit("message")
                        .hasLongGaugeSatisfying(
                            __ ->
                                assertThat(metric.getLongGaugeData().getPoints())
                                    .anySatisfy(
                                        point -> {
                                          assertThat(point.getValue()).isPositive();
                                          assertThat(point.getAttributes())
                                              .containsEntry("pulsar.producer.name", "producer-0");
                                        }))));

    testing.waitAndAssertMetrics(
        PulsarSingletons.INSTRUMENTATION_NAME,
        "pulsarclient.producer.message.send.duration",
        metrics ->
            metrics.anySatisfy(
                metric ->
                    assertThat(metric)
                        .hasDescription("The duration of sent messages")
                        .hasUnit("ms")
                        .hasDoubleGaugeSatisfying(
                            __ ->
                                assertThat(metric.getDoubleGaugeData().getPoints())
                                    .anySatisfy(
                                        point -> {
                                          assertThat(point.getValue()).isPositive();
                                          assertThat(point.getAttributes())
                                              .containsEntry("pulsar.producer.name", "producer-0");
                                        }))));

    testing.waitAndAssertMetrics(
        PulsarSingletons.INSTRUMENTATION_NAME,
        "pulsarclient.consumer.message.received.size",
        metrics ->
            metrics.anySatisfy(
                metric ->
                    assertThat(metric)
                        .hasDescription("Counts the size of received messages")
                        .hasUnit("By")
                        .hasLongGaugeSatisfying(
                            __ ->
                                assertThat(metric.getLongGaugeData().getPoints())
                                    .anySatisfy(
                                        point -> {
                                          assertThat(point.getValue()).isPositive();
                                          assertThat(point.getAttributes())
                                              .containsEntry("pulsar.consumer.name", "consumer-0");
                                          assertThat(point.getAttributes())
                                              .containsEntry("pulsar.subscription", "sub");
                                        }))));

    testing.waitAndAssertMetrics(
        PulsarSingletons.INSTRUMENTATION_NAME,
        "pulsarclient.consumer.message.received.count",
        metrics ->
            metrics.anySatisfy(
                metric ->
                    assertThat(metric)
                        .hasDescription("Counts the number of received messages")
                        .hasUnit("message")
                        .hasLongGaugeSatisfying(
                            __ ->
                                assertThat(metric.getLongGaugeData().getPoints())
                                    .anySatisfy(
                                        point -> {
                                          assertThat(point.getValue()).isPositive();
                                          assertThat(point.getAttributes())
                                              .containsEntry("pulsar.consumer.name", "consumer-0");
                                          assertThat(point.getAttributes())
                                              .containsEntry("pulsar.subscription", "sub");
                                        }))));

    testing.waitAndAssertMetrics(
        PulsarSingletons.INSTRUMENTATION_NAME,
        "pulsarclient.consumer.message.ack.count",
        metrics ->
            metrics.anySatisfy(
                metric ->
                    assertThat(metric)
                        .hasDescription("Counts the number of sent message acknowledgements")
                        .hasUnit("ack")
                        .hasLongGaugeSatisfying(
                            __ ->
                                assertThat(metric.getLongGaugeData().getPoints())
                                    .anySatisfy(
                                        point -> {
                                          assertThat(point.getValue()).isPositive();
                                          assertThat(point.getAttributes())
                                              .containsEntry("pulsar.consumer.name", "consumer-0");
                                          assertThat(point.getAttributes())
                                              .containsEntry("pulsar.subscription", "sub");
                                        }))));

    testing.waitAndAssertMetrics(
        PulsarSingletons.INSTRUMENTATION_NAME,
        "pulsarclient.consumer.receiver.queue.usage",
        metrics ->
            metrics.anySatisfy(
                metric ->
                    assertThat(metric)
                        .hasDescription("Number of the messages in the receiver queue")
                        .hasUnit("message")
                        .hasLongGaugeSatisfying(
                            __ ->
                                assertThat(metric.getLongGaugeData().getPoints())
                                    .anySatisfy(
                                        point -> {
                                          assertThat(point.getValue()).isZero();
                                          assertThat(point.getAttributes())
                                              .containsEntry("pulsar.consumer.name", "consumer-0");
                                          assertThat(point.getAttributes())
                                              .containsEntry("pulsar.subscription", "sub");
                                        }))));

    for (Producer<String> producer : producers) {
      producer.close();
    }
    for (Consumer<String> consumer : consumers) {
      consumer.close();
    }
    assertThat(PulsarMetricsRegistry.getMetricsRegistry().getProducerSize()).isZero();
    assertThat(PulsarMetricsRegistry.getMetricsRegistry().getConsumerSize()).isZero();
  }

  private static List<AttributeAssertion> sendAttributes(
      String destination, String messageId, boolean testHeaders) {
    List<AttributeAssertion> assertions =
        new ArrayList<>(
            Arrays.asList(
                equalTo(SemanticAttributes.MESSAGING_SYSTEM, "pulsar"),
                equalTo(SemanticAttributes.SERVER_ADDRESS, brokerHost),
                equalTo(SemanticAttributes.SERVER_PORT, brokerPort),
                equalTo(SemanticAttributes.MESSAGING_DESTINATION_NAME, destination),
                equalTo(SemanticAttributes.MESSAGING_OPERATION, "publish"),
                equalTo(SemanticAttributes.MESSAGING_MESSAGE_ID, messageId),
                satisfies(
                    SemanticAttributes.MESSAGING_MESSAGE_BODY_SIZE,
                    AbstractLongAssert::isNotNegative),
                equalTo(MESSAGE_TYPE, "normal")));
    if (testHeaders) {
      assertions.add(
          equalTo(
              AttributeKey.stringArrayKey("messaging.header.test_message_header"),
              Collections.singletonList("test")));
    }
    return assertions;
  }

  private static List<AttributeAssertion> batchReceiveAttributes(
      String destination, String messageId, boolean testHeaders) {
    return receiveAttributes(destination, messageId, testHeaders, true);
  }

  private static List<AttributeAssertion> receiveAttributes(
      String destination, String messageId, boolean testHeaders) {
    return receiveAttributes(destination, messageId, testHeaders, false);
  }

  private static List<AttributeAssertion> receiveAttributes(
      String destination, String messageId, boolean testHeaders, boolean isBatch) {
    List<AttributeAssertion> assertions =
        new ArrayList<>(
            Arrays.asList(
                equalTo(SemanticAttributes.MESSAGING_SYSTEM, "pulsar"),
                equalTo(SemanticAttributes.SERVER_ADDRESS, brokerHost),
                equalTo(SemanticAttributes.SERVER_PORT, brokerPort),
                equalTo(SemanticAttributes.MESSAGING_DESTINATION_NAME, destination),
                equalTo(SemanticAttributes.MESSAGING_OPERATION, "receive"),
                equalTo(SemanticAttributes.MESSAGING_MESSAGE_ID, messageId),
                satisfies(
                    SemanticAttributes.MESSAGING_MESSAGE_BODY_SIZE,
                    AbstractLongAssert::isNotNegative)));
    if (testHeaders) {
      assertions.add(
          equalTo(
              AttributeKey.stringArrayKey("messaging.header.test_message_header"),
              Collections.singletonList("test")));
    }
    if (isBatch) {
      assertions.add(
          satisfies(
              SemanticAttributes.MESSAGING_BATCH_MESSAGE_COUNT, AbstractLongAssert::isPositive));
    }
    return assertions;
  }

  private static List<AttributeAssertion> processAttributes(
      String destination, String messageId, boolean testHeaders) {
    List<AttributeAssertion> assertions =
        new ArrayList<>(
            Arrays.asList(
                equalTo(SemanticAttributes.MESSAGING_SYSTEM, "pulsar"),
                equalTo(SemanticAttributes.MESSAGING_DESTINATION_NAME, destination),
                equalTo(SemanticAttributes.MESSAGING_OPERATION, "process"),
                equalTo(SemanticAttributes.MESSAGING_MESSAGE_ID, messageId),
                satisfies(
                    SemanticAttributes.MESSAGING_MESSAGE_BODY_SIZE,
                    AbstractLongAssert::isNotNegative)));
    if (testHeaders) {
      assertions.add(
          equalTo(
              AttributeKey.stringArrayKey("messaging.header.test_message_header"),
              Collections.singletonList("test")));
    }
    return assertions;
  }

  private static void acknowledgeMessage(Consumer<String> consumer, Message<String> message) {
    try {
      consumer.acknowledge(message);
    } catch (PulsarClientException exception) {
      throw new RuntimeException(exception);
    }
  }

  private static void acknowledgeMessages(Consumer<String> consumer, Messages<String> messages) {
    try {
      consumer.acknowledge(messages);
    } catch (PulsarClientException exception) {
      throw new RuntimeException(exception);
    }
  }
}
