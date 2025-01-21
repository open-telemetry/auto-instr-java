/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.runtimemetrics.java8.internal;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.runtimemetrics.java8.Classes;
import io.opentelemetry.instrumentation.runtimemetrics.java8.Cpu;
import io.opentelemetry.instrumentation.runtimemetrics.java8.GarbageCollector;
import io.opentelemetry.instrumentation.runtimemetrics.java8.MemoryPools;
import io.opentelemetry.instrumentation.runtimemetrics.java8.Threads;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public class JmxRuntimeMetricsFactory {
  @SuppressWarnings("CatchingUnchecked")
  public static List<AutoCloseable> buildObservables(
      OpenTelemetry openTelemetry, boolean disableJmx, boolean enableExperimentalJmxTelemetry) {
    if (disableJmx) {
      return Collections.emptyList();
    }
    try {
      // Set up metrics gathered by JMX
      List<AutoCloseable> observables = new ArrayList<>();
      observables.addAll(Classes.registerObservers(openTelemetry));
      observables.addAll(Cpu.registerObservers(openTelemetry));
      observables.addAll(GarbageCollector.registerObservers(openTelemetry));
      observables.addAll(MemoryPools.registerObservers(openTelemetry));
      observables.addAll(Threads.registerObservers(openTelemetry));
      if (enableExperimentalJmxTelemetry) {
        observables.addAll(ExperimentalBufferPools.registerObservers(openTelemetry));
        observables.addAll(ExperimentalCpu.registerObservers(openTelemetry));
        observables.addAll(ExperimentalMemoryPools.registerObservers(openTelemetry));
      }
      return observables;
    } catch (Exception e) {
      throw new IllegalStateException("Error building RuntimeMetrics", e);
    }
  }

  private JmxRuntimeMetricsFactory() {}
}
