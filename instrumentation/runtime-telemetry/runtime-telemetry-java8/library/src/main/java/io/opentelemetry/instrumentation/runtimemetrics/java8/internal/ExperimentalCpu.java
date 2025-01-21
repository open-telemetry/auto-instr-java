/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.runtimemetrics.java8.internal;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.Meter;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;

/**
 * Registers measurements that generate experimental metrics about CPU.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public final class ExperimentalCpu {

  /** Register observers for java runtime experimental CPU metrics. */
  public static List<AutoCloseable> registerObservers(OpenTelemetry openTelemetry) {
    return registerObservers(
        openTelemetry,
        ManagementFactory.getOperatingSystemMXBean(),
        OperatingSystemMethods.systemCpuUtilization());
  }

  // Visible for testing
  static List<AutoCloseable> registerObservers(
      OpenTelemetry openTelemetry,
      OperatingSystemMXBean osBean,
      @Nullable Supplier<Double> systemCpuUtilization) {

    Meter meter = JmxRuntimeMetricsUtil.getMeter(openTelemetry);
    List<AutoCloseable> observables = new ArrayList<>();
    observables.add(
        meter
            .gaugeBuilder("jvm.system.cpu.load_1m")
            .setDescription(
                "Average CPU load of the whole system for the last minute as reported by the JVM.")
            .setUnit("{run_queue_item}")
            .buildWithCallback(
                observableMeasurement -> {
                  double loadAverage = osBean.getSystemLoadAverage();
                  if (loadAverage >= 0) {
                    observableMeasurement.record(loadAverage);
                  }
                }));
    if (systemCpuUtilization != null) {
      observables.add(
          meter
              .gaugeBuilder("jvm.system.cpu.utilization")
              .setDescription("Recent CPU utilization for the whole system as reported by the JVM.")
              .setUnit("1")
              .buildWithCallback(
                  observableMeasurement -> {
                    Double cpuUsage = systemCpuUtilization.get();
                    if (cpuUsage != null && cpuUsage >= 0) {
                      observableMeasurement.record(cpuUsage);
                    }
                  }));
    }
    return observables;
  }

  private ExperimentalCpu() {}
}
