/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal;

import static io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.InstrumentDescriptor.INSTRUMENT_TYPE_DOUBLE_OBSERVABLE_COUNTER;
import static io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.InstrumentDescriptor.INSTRUMENT_TYPE_DOUBLE_OBSERVABLE_GAUGE;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableDoubleMeasurement;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.metrics.KafkaMetric;
import org.apache.kafka.common.metrics.Measurable;

/** A registry mapping kafka metrics to corresponding OpenTelemetry metric definitions. */
final class KafkaMetricRegistry {

  private static final Set<String> groups = new HashSet<>(Arrays.asList("consumer", "producer"));
  private static final Map<Class<?>, String> measurableToInstrumentType = new HashMap<>();
  private static final Map<String, String> descriptionCache = new ConcurrentHashMap<>();

  static {
    Map<String, String> classNameToType = new HashMap<>();
    classNameToType.put(
        "org.apache.kafka.common.metrics.stats.Rate", INSTRUMENT_TYPE_DOUBLE_OBSERVABLE_GAUGE);
    classNameToType.put(
        "org.apache.kafka.common.metrics.stats.Avg", INSTRUMENT_TYPE_DOUBLE_OBSERVABLE_GAUGE);
    classNameToType.put(
        "org.apache.kafka.common.metrics.stats.Max", INSTRUMENT_TYPE_DOUBLE_OBSERVABLE_GAUGE);
    classNameToType.put(
        "org.apache.kafka.common.metrics.stats.Value", INSTRUMENT_TYPE_DOUBLE_OBSERVABLE_GAUGE);
    classNameToType.put(
        "org.apache.kafka.common.metrics.stats.CumulativeSum",
        INSTRUMENT_TYPE_DOUBLE_OBSERVABLE_COUNTER);
    classNameToType.put(
        "org.apache.kafka.common.metrics.stats.CumulativeCount",
        INSTRUMENT_TYPE_DOUBLE_OBSERVABLE_COUNTER);

    for (Map.Entry<String, String> entry : classNameToType.entrySet()) {
      try {
        measurableToInstrumentType.put(Class.forName(entry.getKey()), entry.getValue());
      } catch (ClassNotFoundException e) {
        // Class doesn't exist in this version of kafka client - skip
      }
    }
  }

  @Nullable
  static RegisteredObservable getRegisteredObservable(Meter meter, KafkaMetric kafkaMetric) {
    // If metric is not a Measurable, we can't map it to an instrument
    Class<? extends Measurable> measurable = getMeasurable(kafkaMetric);
    if (measurable == null) {
      return null;
    }
    MetricName metricName = kafkaMetric.metricName();
    Optional<String> matchingGroup =
        groups.stream().filter(group -> metricName.group().contains(group)).findFirst();
    // Only map metrics that have a matching group
    if (!matchingGroup.isPresent()) {
      return null;
    }
    String instrumentName =
        "kafka." + matchingGroup.get() + "." + metricName.name().replace("-", "_");
    String instrumentDescription =
        descriptionCache.computeIfAbsent(instrumentName, s -> metricName.description());
    String instrumentType =
        measurableToInstrumentType.getOrDefault(
            measurable, INSTRUMENT_TYPE_DOUBLE_OBSERVABLE_GAUGE);

    InstrumentDescriptor instrumentDescriptor =
        toInstrumentDescriptor(instrumentType, instrumentName, instrumentDescription);
    Attributes attributes = toAttributes(metricName.tags());
    AutoCloseable observable =
        createObservable(meter, attributes, instrumentDescriptor, kafkaMetric);
    return RegisteredObservable.create(metricName, instrumentDescriptor, attributes, observable);
  }

  @Nullable
  private static Class<? extends Measurable> getMeasurable(KafkaMetric kafkaMetric) {
    try {
      return kafkaMetric.measurable().getClass();
    } catch (IllegalStateException e) {
      return null;
    }
  }

  private static InstrumentDescriptor toInstrumentDescriptor(
      String instrumentType, String instrumentName, String instrumentDescription) {
    switch (instrumentType) {
      case INSTRUMENT_TYPE_DOUBLE_OBSERVABLE_GAUGE:
        return InstrumentDescriptor.createDoubleGauge(instrumentName, instrumentDescription);
      case INSTRUMENT_TYPE_DOUBLE_OBSERVABLE_COUNTER:
        return InstrumentDescriptor.createDoubleCounter(instrumentName, instrumentDescription);
      default: // Continue below to throw
    }
    throw new IllegalStateException("Unrecognized instrument type. This is a bug.");
  }

  private static Attributes toAttributes(Map<String, String> tags) {
    AttributesBuilder attributesBuilder = Attributes.builder();
    tags.forEach(attributesBuilder::put);
    return attributesBuilder.build();
  }

  private static AutoCloseable createObservable(
      Meter meter,
      Attributes attributes,
      InstrumentDescriptor instrumentDescriptor,
      KafkaMetric kafkaMetric) {
    Consumer<ObservableDoubleMeasurement> callback =
        observableMeasurement -> observableMeasurement.record(value(kafkaMetric), attributes);
    switch (instrumentDescriptor.getInstrumentType()) {
      case INSTRUMENT_TYPE_DOUBLE_OBSERVABLE_GAUGE:
        return meter
            .gaugeBuilder(instrumentDescriptor.getName())
            .setDescription(instrumentDescriptor.getDescription())
            .buildWithCallback(callback);
      case INSTRUMENT_TYPE_DOUBLE_OBSERVABLE_COUNTER:
        return meter
            .counterBuilder(instrumentDescriptor.getName())
            .setDescription(instrumentDescriptor.getDescription())
            .ofDoubles()
            .buildWithCallback(callback);
      default: // Continue below to throw
    }
    // TODO: add support for other instrument types and value types as needed for new instruments.
    // This should not happen.
    throw new IllegalStateException("Unrecognized instrument type. This is a bug.");
  }

  private static double value(KafkaMetric kafkaMetric) {
    return kafkaMetric.measurable().measure(kafkaMetric.config(), System.currentTimeMillis());
  }

  private KafkaMetricRegistry() {}
}
