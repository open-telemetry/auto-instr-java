/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.kafkaclients.v2_6.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.AbstractOpenTelemetryMetricsReporterTest;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.OpenTelemetryMetricsReporter;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.OpenTelemetrySupplier;
import io.opentelemetry.instrumentation.kafkaclients.v2_6.KafkaTelemetry;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.LibraryInstrumentationExtension;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.util.Map;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class OpenTelemetryMetricsReporterTest extends AbstractOpenTelemetryMetricsReporterTest {

  @RegisterExtension
  static final InstrumentationExtension testing = LibraryInstrumentationExtension.create();

  @Override
  protected InstrumentationExtension testing() {
    return testing;
  }

  @Override
  protected Map<String, ?> additionalConfig() {
    return KafkaTelemetry.create(testing.getOpenTelemetry()).metricConfigProperties();
  }

  @Test
  void badConfig() {
    Assumptions.assumeFalse(Boolean.getBoolean("testLatestDeps"));

    // Bad producer config
    assertThatThrownBy(
            () -> {
              Map<String, Object> producerConfig = producerConfig();
              producerConfig.remove(OpenTelemetryMetricsReporter.CONFIG_KEY_OPENTELEMETRY_SUPPLIER);
              new KafkaProducer<>(producerConfig).close();
            })
        .hasRootCauseInstanceOf(IllegalStateException.class)
        .hasRootCauseMessage("Missing required configuration property: opentelemetry.supplier");
    assertThatThrownBy(
            () -> {
              Map<String, Object> producerConfig = producerConfig();
              producerConfig.put(
                  OpenTelemetryMetricsReporter.CONFIG_KEY_OPENTELEMETRY_SUPPLIER, "foo");
              new KafkaProducer<>(producerConfig).close();
            })
        .hasRootCauseInstanceOf(IllegalStateException.class)
        .hasRootCauseMessage(
            "Configuration property opentelemetry.supplier is not instance of OpenTelemetrySupplier");
    assertThatThrownBy(
            () -> {
              Map<String, Object> producerConfig = producerConfig();
              producerConfig.remove(
                  OpenTelemetryMetricsReporter.CONFIG_KEY_OPENTELEMETRY_INSTRUMENTATION_NAME);
              new KafkaProducer<>(producerConfig).close();
            })
        .hasRootCauseInstanceOf(IllegalStateException.class)
        .hasRootCauseMessage(
            "Missing required configuration property: opentelemetry.instrumentation_name");
    assertThatThrownBy(
            () -> {
              Map<String, Object> producerConfig = producerConfig();
              producerConfig.put(
                  OpenTelemetryMetricsReporter.CONFIG_KEY_OPENTELEMETRY_INSTRUMENTATION_NAME, 42);
              new KafkaProducer<>(producerConfig).close();
            })
        .hasRootCauseInstanceOf(IllegalStateException.class)
        .hasRootCauseMessage(
            "Configuration property opentelemetry.instrumentation_name is not instance of String");

    // Bad consumer config
    assertThatThrownBy(
            () -> {
              Map<String, Object> consumerConfig = consumerConfig();
              consumerConfig.remove(OpenTelemetryMetricsReporter.CONFIG_KEY_OPENTELEMETRY_SUPPLIER);
              new KafkaConsumer<>(consumerConfig).close();
            })
        .hasRootCauseInstanceOf(IllegalStateException.class)
        .hasRootCauseMessage("Missing required configuration property: opentelemetry.supplier");
    assertThatThrownBy(
            () -> {
              Map<String, Object> consumerConfig = consumerConfig();
              consumerConfig.put(
                  OpenTelemetryMetricsReporter.CONFIG_KEY_OPENTELEMETRY_SUPPLIER, "foo");
              new KafkaConsumer<>(consumerConfig).close();
            })
        .hasRootCauseInstanceOf(IllegalStateException.class)
        .hasRootCauseMessage(
            "Configuration property opentelemetry.supplier is not instance of OpenTelemetrySupplier");
    assertThatThrownBy(
            () -> {
              Map<String, Object> consumerConfig = consumerConfig();
              consumerConfig.remove(
                  OpenTelemetryMetricsReporter.CONFIG_KEY_OPENTELEMETRY_INSTRUMENTATION_NAME);
              new KafkaConsumer<>(consumerConfig).close();
            })
        .hasRootCauseInstanceOf(IllegalStateException.class)
        .hasRootCauseMessage(
            "Missing required configuration property: opentelemetry.instrumentation_name");
    assertThatThrownBy(
            () -> {
              Map<String, Object> consumerConfig = consumerConfig();
              consumerConfig.put(
                  OpenTelemetryMetricsReporter.CONFIG_KEY_OPENTELEMETRY_INSTRUMENTATION_NAME, 42);
              new KafkaConsumer<>(consumerConfig).close();
            })
        .hasRootCauseInstanceOf(IllegalStateException.class)
        .hasRootCauseMessage(
            "Configuration property opentelemetry.instrumentation_name is not instance of String");
  }

  @Test
  void serializableConfig() throws IOException, ClassNotFoundException {
    testSerialize(producerConfig());
    testSerialize(consumerConfig());
  }

  @SuppressWarnings("unchecked")
  private static void testSerialize(Map<String, Object> map)
      throws IOException, ClassNotFoundException {
    OpenTelemetrySupplier supplier =
        (OpenTelemetrySupplier)
            map.get(OpenTelemetryMetricsReporter.CONFIG_KEY_OPENTELEMETRY_SUPPLIER);
    assertThat(supplier).isNotNull();
    assertThat(supplier.get()).isNotNull();
    ByteArrayOutputStream byteOutputStream = new ByteArrayOutputStream();
    try (ObjectOutputStream outputStream = new ObjectOutputStream(byteOutputStream)) {
      outputStream.writeObject(map);
    }

    class CustomObjectInputStream extends ObjectInputStream {
      CustomObjectInputStream(InputStream inputStream) throws IOException {
        super(inputStream);
      }

      @Override
      protected Class<?> resolveClass(ObjectStreamClass desc)
          throws IOException, ClassNotFoundException {
        if (desc.getName().startsWith("io.opentelemetry.")) {
          throw new IllegalStateException(
              "Serial form contains opentelemetry class " + desc.getName());
        }
        return super.resolveClass(desc);
      }
    }

    try (ObjectInputStream inputStream =
        new CustomObjectInputStream(new ByteArrayInputStream(byteOutputStream.toByteArray()))) {
      Map<String, Object> result = (Map<String, Object>) inputStream.readObject();
      assertThat(result.get(OpenTelemetryMetricsReporter.CONFIG_KEY_OPENTELEMETRY_SUPPLIER))
          .isNull();
    }
  }
}
