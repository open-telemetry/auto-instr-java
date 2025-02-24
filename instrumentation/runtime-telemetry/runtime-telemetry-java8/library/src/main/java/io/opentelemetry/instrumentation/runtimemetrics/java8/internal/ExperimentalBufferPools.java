/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.runtimemetrics.java8.internal;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.metrics.ObservableLongMeasurement;
import java.lang.management.BufferPoolMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Registers measurements that generate experimental metrics about buffer pools.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public final class ExperimentalBufferPools {

  private static final AttributeKey<String> JVM_BUFFER_POOL_NAME =
      stringKey("jvm.buffer.pool.name");

  /** Register observers for java runtime buffer pool metrics. */
  public static List<AutoCloseable> registerObservers(OpenTelemetry openTelemetry) {
    List<BufferPoolMXBean> bufferBeans =
        ManagementFactory.getPlatformMXBeans(BufferPoolMXBean.class);
    return registerObservers(openTelemetry, bufferBeans);
  }

  // Visible for testing
  static List<AutoCloseable> registerObservers(
      OpenTelemetry openTelemetry, List<BufferPoolMXBean> bufferBeans) {

    List<AutoCloseable> observables = new ArrayList<>();
    Meter meter = JmxRuntimeMetricsUtil.getMeter(openTelemetry);
    observables.add(
        meter
            .upDownCounterBuilder("jvm.buffer.memory.used")
            .setDescription("Measure of memory used by buffers.")
            .setUnit("By")
            .buildWithCallback(callback(bufferBeans, BufferPoolMXBean::getMemoryUsed)));
    observables.add(
        meter
            .upDownCounterBuilder("jvm.buffer.memory.limit")
            .setDescription("Measure of total memory capacity of buffers.")
            .setUnit("By")
            .buildWithCallback(callback(bufferBeans, BufferPoolMXBean::getTotalCapacity)));
    observables.add(
        meter
            .upDownCounterBuilder("jvm.buffer.count")
            .setDescription("Number of buffers in the pool.")
            .setUnit("{buffer}")
            .buildWithCallback(callback(bufferBeans, BufferPoolMXBean::getCount)));
    return observables;
  }

  // Visible for testing
  static Consumer<ObservableLongMeasurement> callback(
      List<BufferPoolMXBean> bufferPools, Function<BufferPoolMXBean, Long> extractor) {
    List<Attributes> attributeSets = new ArrayList<>(bufferPools.size());
    for (BufferPoolMXBean pool : bufferPools) {
      attributeSets.add(Attributes.builder().put(JVM_BUFFER_POOL_NAME, pool.getName()).build());
    }
    return measurement -> {
      for (int i = 0; i < bufferPools.size(); i++) {
        Attributes attributes = attributeSets.get(i);
        long value = extractor.apply(bufferPools.get(i));
        if (value != -1) {
          measurement.record(value, attributes);
        }
      }
    };
  }

  private ExperimentalBufferPools() {}
}
