/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.kafkaclients.v0_11;

import static java.util.Collections.emptyMap;

import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.AbstractOpenTelemetryMetricsReporterTest;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import java.util.Collections;
import java.util.Map;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.extension.RegisterExtension;

@EnabledIfSystemProperty(
    named = "testLatestDeps",
    matches = "true",
    disabledReason =
        "kafka-clients 0.11 emits a significantly different set of metrics; it's probably fine to just test the latest version")
class OpenTelemetryMetricsReporterTest extends AbstractOpenTelemetryMetricsReporterTest {

  @RegisterExtension
  static final InstrumentationExtension testing = AgentInstrumentationExtension.create();

  @Override
  protected InstrumentationExtension testing() {
    return testing;
  }

  @Override
  protected Map<String, ?> additionalConfig() {
    return emptyMap();
  }

  @Test
  void emptyMetricsReporter() {
    Map<String, Object> consumerConfig = consumerConfig();
    consumerConfig.put(CommonClientConfigs.METRIC_REPORTER_CLASSES_CONFIG, "");
    new KafkaConsumer<>(consumerConfig).close();

    Map<String, Object> producerConfig = producerConfig();
    producerConfig.put(CommonClientConfigs.METRIC_REPORTER_CLASSES_CONFIG, "");
    new KafkaProducer<>(producerConfig).close();
  }

  @Test
  void classListMetricsReporter() {
    Map<String, Object> consumerConfig = consumerConfig();
    consumerConfig.put(
        CommonClientConfigs.METRIC_REPORTER_CLASSES_CONFIG,
        Collections.singletonList(TestMetricsReporter.class));
    new KafkaConsumer<>(consumerConfig).close();

    Map<String, Object> producerConfig = producerConfig();
    producerConfig.put(
        CommonClientConfigs.METRIC_REPORTER_CLASSES_CONFIG,
        Collections.singletonList(TestMetricsReporter.class));
    new KafkaProducer<>(producerConfig).close();
  }

  @Test
  void stringListMetricsReporter() {
    Map<String, Object> consumerConfig = consumerConfig();
    consumerConfig.put(
        CommonClientConfigs.METRIC_REPORTER_CLASSES_CONFIG,
        Collections.singletonList(TestMetricsReporter.class.getName()));
    new KafkaConsumer<>(consumerConfig).close();

    Map<String, Object> producerConfig = producerConfig();
    producerConfig.put(
        CommonClientConfigs.METRIC_REPORTER_CLASSES_CONFIG,
        Collections.singletonList(TestMetricsReporter.class.getName()));
    new KafkaProducer<>(producerConfig).close();
  }
}
