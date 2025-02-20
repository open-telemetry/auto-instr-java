/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.rabbitmq;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.satisfies;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Consumer;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.GetResponse;
import com.rabbitmq.client.ShutdownSignalException;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.api.semconv.network.internal.NetworkAttributes;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.LibraryInstrumentationExtension;
import io.opentelemetry.sdk.testing.assertj.SpanDataAssert;
import io.opentelemetry.sdk.testing.assertj.TraceAssert;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.semconv.SemanticAttributes;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class RabbitMqInstrumentedTest extends AbstractRabbitMqTest {
  @RegisterExtension
  private static final InstrumentationExtension testing = LibraryInstrumentationExtension.create();

  Connection conn;
  Channel channel;

  RabbitTelemetry rabbitTelemetry =
      new RabbitTelemetryBuilder(testing.getOpenTelemetry())
          .setCapturedHeaders(singletonList("test-message-header"))
          .setCaptureExperimentalSpanAttributes(true)
          .build();

  @BeforeEach
  public void setup() throws IOException, TimeoutException {
    conn = connectionFactory.newConnection();
    channel = conn.createChannel();
  }

  @AfterEach
  public void cleanup() throws IOException, TimeoutException {
    try {
      if (null != channel) {
        channel.close();
      }
      if (null != conn) {
        conn.close();
      }
    } catch (ShutdownSignalException ignored) {
    }
  }

  @Test
  void testRabbitPublishGet() throws IOException {
    String exchangeName = "some-exchange";
    String routingKey = "some-routing-key";
    InstrumentedChannel instrumentedChannel =
        new InstrumentedChannel(rabbitTelemetry, channel, exchangeName, routingKey);

    String queueName =
        testing.runWithSpan(
            "producer parent",
            () -> {
              channel.exchangeDeclare(exchangeName, "direct", false);
              String internalQueueName = channel.queueDeclare().getQueue();
              channel.queueBind(internalQueueName, exchangeName, routingKey);
              instrumentedChannel.publish("Hello, world!".getBytes(Charset.defaultCharset()));
              return internalQueueName;
            });
    GetResponse response =
        testing.runWithSpan("consumer parent", () -> instrumentedChannel.basicGet(queueName, true));

    assertEquals("Hello, world!", new String(response.getBody(), Charset.defaultCharset()));

    AtomicReference<SpanData> producerSpan = new AtomicReference<>();

    testing.waitAndAssertTraces(
        trace ->
            trace
                .hasSize(2)
                .hasSpansSatisfyingExactly(
                    span ->
                        span.hasName("producer parent").hasKind(SpanKind.INTERNAL).hasNoParent(),
                    span -> {
                      verifySpan(
                          trace,
                          span,
                          1,
                          exchangeName,
                          routingKey,
                          "publish",
                          exchangeName,
                          trace.getSpan(0));
                      producerSpan.set(trace.getSpan(1));
                    }),
        trace ->
            trace
                .hasSize(2)
                .hasSpansSatisfyingExactly(
                    span ->
                        span.hasName("consumer parent").hasKind(SpanKind.INTERNAL).hasNoParent(),
                    span ->
                        verifySpan(
                            trace,
                            span,
                            1,
                            exchangeName,
                            routingKey,
                            "receive",
                            "<generated>",
                            trace.getSpan(0),
                            producerSpan.get(),
                            null,
                            null,
                            false)));
  }

  @Test
  void testRabbitPublishGetWithDefaultExchange() throws IOException {
    String queueName = channel.queueDeclare().getQueue();
    InstrumentedChannel instrumentedChannel =
        new InstrumentedChannel(rabbitTelemetry, channel, "", queueName);

    testing.runWithSpan(
        "producer parent",
        () -> instrumentedChannel.publish("Hello, world!".getBytes(Charset.defaultCharset())));
    GetResponse response =
        testing.runWithSpan("consumer parent", () -> instrumentedChannel.basicGet(queueName, true));

    assertEquals("Hello, world!", new String(response.getBody(), Charset.defaultCharset()));

    AtomicReference<SpanData> producerSpan = new AtomicReference<>();

    testing.waitAndAssertTraces(
        trace ->
            trace
                .hasSize(2)
                .hasSpansSatisfyingExactly(
                    span ->
                        span.hasName("producer parent").hasKind(SpanKind.INTERNAL).hasNoParent(),
                    span -> {
                      verifySpan(
                          trace,
                          span,
                          1,
                          "<default>",
                          null,
                          "publish",
                          "<default>",
                          trace.getSpan(0));
                      producerSpan.set(trace.getSpan(1));
                    }),
        trace ->
            trace
                .hasSize(2)
                .hasSpansSatisfyingExactly(
                    span ->
                        span.hasName("consumer parent").hasKind(SpanKind.INTERNAL).hasNoParent(),
                    span ->
                        verifySpan(
                            trace,
                            span,
                            1,
                            "<default>",
                            null,
                            "receive",
                            "<generated>",
                            trace.getSpan(0),
                            producerSpan.get(),
                            null,
                            null,
                            false)));
  }

  @ParameterizedTest(name = "test rabbit consume {1} messages and setTimestamp={2}")
  @MethodSource("provideParametersForMessageCountAndTimestamp")
  void testRabbitConsumeMessageCountAndSetTimestamp(
      String exchangeName, int messageCount, boolean setTimestamp) throws IOException {
    channel.exchangeDeclare(exchangeName, "direct", false);

    String queueName =
        (messageCount % 2 == 0)
            ? channel.queueDeclare().getQueue()
            : channel.queueDeclare("some-queue", false, true, true, null).getQueue();
    channel.queueBind(queueName, exchangeName, "");
    InstrumentedChannel instrumentedChannel =
        new InstrumentedChannel(rabbitTelemetry, channel, exchangeName, "");

    List<String> deliveries = new ArrayList<>();

    Consumer callback =
        new DefaultConsumer(channel) {
          @Override
          public void handleDelivery(
              String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
              throws IOException {
            deliveries.add(new String(body, Charset.defaultCharset()));
          }
        };

    TracedDelegatingConsumer instrumentedConsumer =
        new TracedDelegatingConsumer(queueName, callback, rabbitTelemetry);

    channel.basicConsume(queueName, instrumentedConsumer);

    for (int i = 1; i <= messageCount; i++) {
      if (setTimestamp) {
        instrumentedChannel.publish(
            ("msg " + i).getBytes(Charset.defaultCharset()),
            new AMQP.BasicProperties.Builder().timestamp(new Date()).build());
      } else {
        instrumentedChannel.publish(("msg " + i).getBytes(Charset.defaultCharset()));
      }
    }

    String resource = messageCount % 2 == 0 ? "<generated>" : queueName;

    List<java.util.function.Consumer<TraceAssert>> traceAssertions = new ArrayList<>();

    for (int i = 1; i <= messageCount; i++) {
      traceAssertions.add(
          trace ->
              trace
                  .hasSize(2)
                  .hasSpansSatisfyingExactly(
                      span ->
                          verifySpan(trace, span, 0, exchangeName, null, "publish", exchangeName),
                      span ->
                          verifySpan(
                              trace,
                              span,
                              1,
                              exchangeName,
                              null,
                              "process",
                              resource,
                              trace.getSpan(0),
                              null,
                              null,
                              null,
                              setTimestamp)));
    }

    testing.waitAndAssertTraces(traceAssertions);

    assertEquals(messageCount, deliveries.size());
    for (int i = 1; i <= messageCount; i++) {
      assertEquals("msg " + i, deliveries.get(i - 1));
    }
  }

  @Test
  void testRabbitConsumeError() throws IOException {
    String exchangeName = "some-error-exchange";
    FileNotFoundException error = new FileNotFoundException("Message Error");
    channel.exchangeDeclare(exchangeName, "direct", false);
    String queueName = channel.queueDeclare().getQueue();
    channel.queueBind(queueName, exchangeName, "");

    InstrumentedChannel instrumentedChannel =
        new InstrumentedChannel(rabbitTelemetry, channel, exchangeName, "");

    Consumer callback =
        new DefaultConsumer(channel) {
          @Override
          public void handleDelivery(
              String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body)
              throws IOException {
            throw error;
            // Unfortunately this doesn't seem to be observable in the test outside of the span
            // generated.
          }
        };

    TracedDelegatingConsumer instrumentedConsumer =
        new TracedDelegatingConsumer(queueName, callback, rabbitTelemetry);
    // TODO review Consumer headers how to get custom headers in DeliveryRequest or AMQP.Properties
    channel.basicConsume(queueName, instrumentedConsumer);

    instrumentedChannel.publish("msg".getBytes(Charset.defaultCharset()));

    testing.waitAndAssertTraces(
        trace ->
            trace
                .hasSize(2)
                .hasSpansSatisfyingExactly(
                    span -> verifySpan(trace, span, 0, exchangeName, null, "publish", exchangeName),
                    span ->
                        verifySpan(
                            trace,
                            span,
                            1,
                            exchangeName,
                            null,
                            "process",
                            "<generated>",
                            trace.getSpan(0),
                            null,
                            error,
                            error.getMessage(),
                            false)));
  }

  @Test
  void captureMessageHeaderAsSpanAttributes() throws IOException, InterruptedException {
    String queueName = channel.queueDeclare().getQueue();
    InstrumentedChannel instrumentedChannel =
        new InstrumentedChannel(rabbitTelemetry, channel, "", queueName);

    Map<String, Object> headers = new java.util.HashMap<>();
    headers.put("test-message-header", "test");
    AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder().headers(headers).build();
    instrumentedChannel.publish("Hello, world!".getBytes(Charset.defaultCharset()), properties);

    CountDownLatch latch = new CountDownLatch(1);
    List<String> deliveries = new ArrayList<>();

    Consumer callback =
        new DefaultConsumer(channel) {
          @Override
          public void handleDelivery(
              String consumerTag, Envelope envelope, AMQP.BasicProperties props, byte[] body)
              throws IOException {
            deliveries.add(new String(body, Charset.defaultCharset()));
            latch.countDown();
          }
        };

    TracedDelegatingConsumer instrumentedConsumer =
        new TracedDelegatingConsumer(queueName, callback, rabbitTelemetry);
    channel.basicConsume(queueName, instrumentedConsumer);
    assertTrue(latch.await(10, TimeUnit.SECONDS));

    assertEquals("Hello, world!", deliveries.get(0));

    testing.waitAndAssertTraces(
        trace ->
            trace
                .hasSize(2)
                .hasSpansSatisfyingExactly(
                    span -> {
                      verifySpan(trace, span, 0, "<default>", null, "publish", "<default>");
                      span.hasAttributesSatisfying(
                          attributes ->
                              assertThat(attributes)
                                  .satisfies(
                                      attrs -> {
                                        List<String> verifyHeaders =
                                            attrs.get(
                                                AttributeKey.stringArrayKey(
                                                    "messaging.header.test_message_header"));
                                        assertNotNull(verifyHeaders);
                                        assertTrue(verifyHeaders.contains("test"));
                                      }));
                    },
                    span -> {
                      verifySpan(
                          trace,
                          span,
                          1,
                          "<default>",
                          null,
                          "process",
                          "<generated>",
                          trace.getSpan(0));
                      span.hasAttributesSatisfying(
                          attributes -> {
                            assertThat(attributes)
                                .satisfies(
                                    attrs -> {
                                      List<String> verifyHeaders =
                                          attrs.get(
                                              AttributeKey.stringArrayKey(
                                                  "messaging.header.test_message_header"));
                                      assertNotNull(verifyHeaders);
                                      assertTrue(verifyHeaders.contains("test"));
                                    });
                          });
                    }));
  }

  private static Stream<Arguments> provideParametersForMessageCountAndTimestamp() {
    return Stream.of(
        Arguments.of("some-exchange", 1, false),
        Arguments.of("some-exchange", 2, false),
        Arguments.of("some-exchange", 3, false),
        Arguments.of("some-exchange", 4, false),
        Arguments.of("some-exchange", 1, true),
        Arguments.of("some-exchange", 2, true),
        Arguments.of("some-exchange", 3, true),
        Arguments.of("some-exchange", 4, true));
  }

  private static void verifySpan(
      TraceAssert trace,
      SpanDataAssert span,
      int index,
      String exchange,
      String routingKey,
      String operation,
      String resource) {
    verifySpan(trace, span, index, exchange, routingKey, operation, resource, null);
  }

  private static void verifySpan(
      TraceAssert trace,
      SpanDataAssert span,
      int index,
      String exchange,
      String routingKey,
      String operation,
      String resource,
      SpanData parentSpan) {
    verifySpan(
        trace,
        span,
        index,
        exchange,
        routingKey,
        operation,
        resource,
        parentSpan,
        null,
        null,
        null,
        false);
  }

  private static void verifySpan(
      TraceAssert trace,
      SpanDataAssert span,
      int index,
      String exchange,
      String routingKey,
      String operation,
      String resource,
      SpanData parentSpan,
      SpanData linkSpan,
      Throwable exception,
      String errorMsg,
      boolean expectTimestamp) {
    String spanName = resource;
    if (operation != null) {
      spanName += " " + operation;
    }

    String rabbitCommand =
        trace.getSpan(index).getAttributes().get(AttributeKey.stringKey("rabbitmq.command"));

    SpanKind spanKind = captureSpanKind(rabbitCommand);

    span.hasName(spanName).hasKind(spanKind);

    verifyParentAndLink(span, parentSpan, linkSpan);

    if (null != exception) {
      verifyException(span, exception, errorMsg);
    }

    // listener does not have access to net attributes
    if (!"basic.deliver".equals(rabbitCommand)) {
      verifyNetAttributes(span);
    }

    verifyMessagingAttributes(span, exchange, routingKey, operation);

    if (expectTimestamp) {
      span.hasAttributesSatisfying(
          attributes -> {
            assertThat(attributes)
                .satisfies(
                    attrs -> {
                      Long timestamp =
                          attrs.get(AttributeKey.longKey("rabbitmq.record.queue_time_ms"));
                      assertNotNull(timestamp);
                      assertTrue(timestamp >= 0);
                    });
          });
    }

    if (null != rabbitCommand) {
      switch (rabbitCommand) {
        case "basic.publish":
          span.hasAttribute(AttributeKey.stringKey("rabbitmq.command"), "basic.publish")
              .hasAttributesSatisfying(
                  attributes -> {
                    assertThat(attributes)
                        .satisfies(
                            attrs -> {
                              String routingKeyAttr =
                                  attrs.get(
                                      SemanticAttributes
                                          .MESSAGING_RABBITMQ_DESTINATION_ROUTING_KEY);
                              assertTrue(
                                  routingKeyAttr == null
                                      || routingKeyAttr.equals("some-routing-key")
                                      || routingKeyAttr.equals("some-routing-queue")
                                      || routingKeyAttr.startsWith("amq.gen-"));

                              Long deliveryMode =
                                  attrs.get(AttributeKey.longKey("rabbitmq.delivery_mode"));
                              assertTrue(deliveryMode == null || deliveryMode == 2);

                              assertNotNull(
                                  attrs.get(
                                      SemanticAttributes.MESSAGING_MESSAGE_PAYLOAD_SIZE_BYTES));
                            });
                  });
          break;
        case "basic.get":
          span.hasAttribute(AttributeKey.stringKey("rabbitmq.command"), "basic.get")
              .hasAttributesSatisfying(
                  attributes -> {
                    assertThat(attributes)
                        .satisfies(
                            attrs -> {
                              // TODO why this queue name is not a destination for semantic
                              // convention
                              String queue = attrs.get(AttributeKey.stringKey("rabbitmq.queue"));
                              assertNotNull(queue);
                              assertTrue(
                                  queue.equals("some-queue")
                                      || queue.equals("some-routing-queue")
                                      || queue.startsWith("amq.gen-"));

                              attrs.get(SemanticAttributes.MESSAGING_MESSAGE_PAYLOAD_SIZE_BYTES);
                            });
                  });
          break;
        case "basic.deliver":
          span.hasAttribute(AttributeKey.stringKey("rabbitmq.command"), "basic.deliver")
              .hasAttributesSatisfying(
                  attributes -> {
                    assertThat(attributes)
                        .satisfies(
                            attrs -> {
                              attrs.get(SemanticAttributes.MESSAGING_MESSAGE_PAYLOAD_SIZE_BYTES);
                            });
                  });
          break;
        default:
          span.hasAttributesSatisfying(
              attributes -> {
                assertThat(attributes)
                    .satisfies(
                        attrs -> {
                          String command = attrs.get(AttributeKey.stringKey("rabbitmq.command"));
                          assertTrue(command == null || command.equals(resource));
                        });
              });
      }
    } else {
      span.hasAttributesSatisfying(
          attributes -> {
            assertThat(attributes)
                .satisfies(
                    attrs -> {
                      String command = attrs.get(AttributeKey.stringKey("rabbitmq.command"));
                      assertTrue(command == null || command.equals(resource));
                    });
          });
    }
  }

  // Ignoring deprecation warning for use of SemanticAttributes
  @SuppressWarnings("deprecation")
  private static void verifyMessagingAttributes(
      SpanDataAssert span, String exchange, String routingKey, String operation) {
    span.hasAttribute(SemanticAttributes.MESSAGING_SYSTEM, "rabbitmq")
        .hasAttributesSatisfying(
            attributes -> {
              assertThat(attributes)
                  .satisfies(
                      attrs -> {
                        String destinationName =
                            attrs.get(SemanticAttributes.MESSAGING_DESTINATION);
                        assertTrue(destinationName == null || destinationName.equals(exchange));
                        String routingKeyAttr =
                            attrs.get(
                                SemanticAttributes.MESSAGING_RABBITMQ_DESTINATION_ROUTING_KEY);
                        assertTrue(
                            routingKeyAttr == null
                                || routingKeyAttr.equals(routingKey)
                                || routingKeyAttr.startsWith("amq.gen-"));
                      });
            });

    if (operation != null && !operation.equals("publish")) {
      span.hasAttribute(SemanticAttributes.MESSAGING_OPERATION, operation);
    }
  }

  private static SpanKind captureSpanKind(String rabbitCommand) {
    SpanKind spanKind = SpanKind.CLIENT;

    if (null != rabbitCommand) {
      switch (rabbitCommand) {
        case "basic.publish":
          spanKind = SpanKind.PRODUCER;
          break;
        case "basic.get": // fallthrough
        case "basic.deliver":
          spanKind = SpanKind.CONSUMER;
          break;
        default:
          break;
      }
    }

    return spanKind;
  }

  private static void verifyNetAttributes(SpanDataAssert span) {
    span.hasAttributesSatisfying(
        attributes -> {
          assertThat(attributes)
              .satisfies(
                  attrs -> {
                    String peerAddr = attrs.get(NetworkAttributes.NETWORK_PEER_ADDRESS);
                    assertTrue(
                        "127.0.0.1".equals(peerAddr)
                            || "0:0:0:0:0:0:0:1".equals(peerAddr)
                            || peerAddr == null);

                    String networkType = attrs.get(SemanticAttributes.NETWORK_TYPE);
                    assertThat(networkType).isIn("ipv4", "ipv6", null);

                    assertNotNull(attrs.get(NetworkAttributes.NETWORK_PEER_PORT));
                  });
        });
  }

  private static void verifyException(SpanDataAssert span, Throwable exception, String errorMsg) {
    span.hasStatus(StatusData.error())
        .hasEventsSatisfying(
            events -> {
              assertThat(events.get(0))
                  .hasName(SemanticAttributes.EXCEPTION_EVENT_NAME)
                  .hasAttributesSatisfying(
                      equalTo(SemanticAttributes.EXCEPTION_TYPE, exception.getClass().getName()),
                      equalTo(SemanticAttributes.EXCEPTION_MESSAGE, errorMsg),
                      satisfies(
                          SemanticAttributes.EXCEPTION_STACKTRACE,
                          val -> val.isInstanceOf(String.class)));
            });
  }

  private static void verifyParentAndLink(
      SpanDataAssert span, SpanData parentSpan, SpanData linkSpan) {
    if (null != parentSpan) {
      span.hasParent(parentSpan);
    } else {
      span.hasNoParent();
    }

    if (null != linkSpan) {
      // create from remote context
      span.hasLinks(
          LinkData.create(
              SpanContext.createFromRemoteParent(
                  linkSpan.getSpanContext().getTraceId(),
                  linkSpan.getSpanContext().getSpanId(),
                  linkSpan.getSpanContext().getTraceFlags(),
                  linkSpan.getSpanContext().getTraceState())));
    } else {
      span.hasTotalRecordedLinks(0);
    }
  }
}
