/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.spring.pulsar.v1_0;

import static io.opentelemetry.javaagent.instrumentation.spring.pulsar.v1_0.SpringPulsarTest.ConsumerConfig.OTEL_TOPIC;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.satisfies;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_ADDRESS;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_PORT;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_BATCH_MESSAGE_COUNT;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_DESTINATION_NAME;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_MESSAGE_BODY_SIZE;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_MESSAGE_ID;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_OPERATION;
import static io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes.MESSAGING_SYSTEM;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.testing.GlobalTraceUtil;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.assertj.core.api.AbstractLongAssert;
import org.assertj.core.api.AbstractStringAssert;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.pulsar.annotation.PulsarListener;
import org.springframework.pulsar.core.PulsarTemplate;
import org.testcontainers.containers.PulsarContainer;
import org.testcontainers.utility.DockerImageName;

public class SpringPulsarTest {

  @RegisterExtension
  private static final InstrumentationExtension testing = AgentInstrumentationExtension.create();

  private static final DockerImageName DEFAULT_IMAGE_NAME =
      DockerImageName.parse("apachepulsar/pulsar:4.0.2");
  private static PulsarContainer pulsarContainer;
  private static ConfigurableApplicationContext applicationContext;
  private static PulsarTemplate<String> pulsarTemplate;
  static String brokerHost;
  static int brokerPort;
  static PulsarClient client;
  static String ip;

  @BeforeAll
  @SuppressWarnings("unchecked")
  static void setUp() throws PulsarClientException, UnknownHostException {
    pulsarContainer =
        new PulsarContainer(DEFAULT_IMAGE_NAME)
            .withEnv("PULSAR_MEM", "-Xmx128m")
            .withStartupTimeout(Duration.ofMinutes(2));
    pulsarContainer.start();
    brokerHost = pulsarContainer.getHost();
    brokerPort = pulsarContainer.getMappedPort(6650);

    SpringApplication app = new SpringApplication(ConsumerConfig.class);
    Map<String, Object> props = new HashMap<>();
    props.put("spring.main.web-application-type", "none");
    props.put("spring.pulsar.client.service-url", pulsarContainer.getPulsarBrokerUrl());
    props.put("spring.pulsar.consumer.subscription.initial-position", "earliest");
    app.setDefaultProperties(props);
    applicationContext = app.run();
    pulsarTemplate = applicationContext.getBean(PulsarTemplate.class);

    client = PulsarClient.builder().serviceUrl(pulsarContainer.getPulsarBrokerUrl()).build();
    ip = InetAddress.getByName(pulsarContainer.getHost()).getHostAddress();
  }

  @AfterAll
  static void teardown() {
    if (pulsarContainer != null) {
      pulsarContainer.stop();
    }
    if (applicationContext != null) {
      applicationContext.close();
    }
  }

  @Test
  @SuppressWarnings("deprecation") // using deprecated semconv
  public void shouldCreateSpansForMessageProcess() {
    testing.runWithSpan(
        "parent",
        () -> {
          pulsarTemplate.send(OTEL_TOPIC, "test");
        });
    testing.waitAndAssertTraces(
        trace -> {
          trace.hasSpansSatisfyingExactly(
              span -> span.hasName("parent").hasKind(SpanKind.INTERNAL),
              span ->
                  span.hasName(OTEL_TOPIC + " publish")
                      .hasKind(SpanKind.PRODUCER)
                      .hasParent(trace.getSpan(0))
                      .hasAttributesSatisfyingExactly(
                          equalTo(MESSAGING_SYSTEM, "pulsar"),
                          equalTo(MESSAGING_OPERATION, "publish"),
                          equalTo(MESSAGING_DESTINATION_NAME, OTEL_TOPIC),
                          satisfies(MESSAGING_MESSAGE_BODY_SIZE, AbstractLongAssert::isNotNegative),
                          satisfies(MESSAGING_MESSAGE_ID, AbstractStringAssert::isNotEmpty),
                          equalTo(SERVER_ADDRESS, brokerHost),
                          equalTo(SERVER_PORT, brokerPort)),
              span ->
                  span.hasName(String.format("%s process", OTEL_TOPIC))
                      .hasKind(SpanKind.CONSUMER)
                      .hasParent(trace.getSpan(1))
                      .hasAttributesSatisfyingExactly(
                          equalTo(MESSAGING_SYSTEM, "pulsar"),
                          equalTo(MESSAGING_OPERATION, "process"),
                          satisfies(MESSAGING_MESSAGE_BODY_SIZE, AbstractLongAssert::isNotNegative),
                          satisfies(MESSAGING_MESSAGE_ID, AbstractStringAssert::isNotEmpty),
                          equalTo(MESSAGING_DESTINATION_NAME, OTEL_TOPIC)),
              span -> {
                span.hasName("consumer").hasKind(SpanKind.INTERNAL).hasParent(trace.getSpan(2));
              });
        },
        trace -> {
          trace.hasSpansSatisfyingExactly(
              span ->
                  span.hasName(String.format("%s receive", OTEL_TOPIC))
                      .hasKind(SpanKind.CONSUMER)
                      .hasAttributesSatisfyingExactly(
                          equalTo(MESSAGING_SYSTEM, "pulsar"),
                          equalTo(MESSAGING_OPERATION, "receive"),
                          equalTo(MESSAGING_DESTINATION_NAME, OTEL_TOPIC),
                          equalTo(SERVER_ADDRESS, brokerHost),
                          satisfies(
                              MESSAGING_BATCH_MESSAGE_COUNT, AbstractLongAssert::isNotNegative),
                          satisfies(MESSAGING_MESSAGE_BODY_SIZE, AbstractLongAssert::isNotNegative),
                          equalTo(SERVER_PORT, brokerPort)));
        });
  }

  @SpringBootConfiguration
  @EnableAutoConfiguration
  static class ConsumerConfig {
    static final String OTEL_TOPIC = "persistent://public/default/otel-topic";
    static final String OTEL_SUBSCRIPTION = "otel-subscription";

    @PulsarListener(subscriptionName = OTEL_SUBSCRIPTION, topics = OTEL_TOPIC)
    void consumer(String ignored) {
      GlobalTraceUtil.runWithSpan("consumer", () -> {});
    }
  }
}
