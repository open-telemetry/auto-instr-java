/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.runtimemetrics.java17.internal.buffer;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.instrumentation.runtimemetrics.java17.JfrFeature;
import io.opentelemetry.instrumentation.runtimemetrics.java17.internal.Constants;
import io.opentelemetry.instrumentation.runtimemetrics.java17.internal.RecordedEventHandler;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import jdk.jfr.consumer.RecordedEvent;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class DirectBufferStatisticsHandler implements RecordedEventHandler {
  private static final String METRIC_NAME_USAGE = "jvm.buffer.memory.used";
  private static final String METRIC_NAME_LIMIT = "jvm.buffer.memory.limit";
  private static final String METRIC_NAME_COUNT = "jvm.buffer.count";
  private static final String METRIC_DESCRIPTION_USAGE = "Measure of memory used by buffers.";
  private static final String METRIC_DESCRIPTION_LIMIT =
      "Measure of total memory capacity of buffers.";
  private static final String METRIC_DESCRIPTION_COUNT = "Number of buffers in the pool.";
  private static final String COUNT = "count";
  private static final String MAX_CAPACITY = "maxCapacity";
  private static final String MEMORY_USED = "memoryUsed";

  private static final String EVENT_NAME = "jdk.DirectBufferStatistics";
  public static final AttributeKey<String> ATTR_BUFFER_POOL =
      AttributeKey.stringKey("jvm.buffer.pool.name");
  private static final Attributes ATTR = Attributes.of(ATTR_BUFFER_POOL, "direct");

  private final List<AutoCloseable> observables = new ArrayList<>();

  private volatile long usage = 0;
  private volatile long limit = 0;
  private volatile long count = 0;

  public DirectBufferStatisticsHandler(Meter meter) {
    observables.add(
        meter
            .upDownCounterBuilder(METRIC_NAME_USAGE)
            .setDescription(METRIC_DESCRIPTION_USAGE)
            .setUnit(Constants.BYTES)
            .buildWithCallback(measurement -> measurement.record(usage, ATTR)));
    observables.add(
        meter
            .upDownCounterBuilder(METRIC_NAME_LIMIT)
            .setDescription(METRIC_DESCRIPTION_LIMIT)
            .setUnit(Constants.BYTES)
            .buildWithCallback(measurement -> measurement.record(limit, ATTR)));
    observables.add(
        meter
            .upDownCounterBuilder(METRIC_NAME_COUNT)
            .setDescription(METRIC_DESCRIPTION_COUNT)
            .setUnit(Constants.UNIT_BUFFERS)
            .buildWithCallback(measurement -> measurement.record(count, ATTR)));
  }

  @Override
  public void accept(RecordedEvent ev) {
    if (ev.hasField(COUNT)) {
      count = ev.getLong(COUNT);
    }
    if (ev.hasField(MAX_CAPACITY)) {
      limit = ev.getLong(MAX_CAPACITY);
    }
    if (ev.hasField(MEMORY_USED)) {
      usage = ev.getLong(MEMORY_USED);
    }
  }

  @Override
  public String getEventName() {
    return EVENT_NAME;
  }

  @Override
  public JfrFeature getFeature() {
    return JfrFeature.BUFFER_METRICS;
  }

  @Override
  public Optional<Duration> getPollingDuration() {
    return Optional.of(Duration.ofSeconds(1));
  }

  @Override
  public void close() {
    RecordedEventHandler.closeObservables(observables);
  }
}
