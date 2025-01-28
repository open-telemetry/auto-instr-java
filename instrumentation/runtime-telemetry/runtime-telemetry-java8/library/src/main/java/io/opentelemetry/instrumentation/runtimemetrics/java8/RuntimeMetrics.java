/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.runtimemetrics.java8;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.runtimemetrics.java8.internal.JmxRuntimeMetricsUtil;
import java.io.Closeable;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

/** The entry point class for runtime metrics support using JFR and JMX. */
public final class RuntimeMetrics implements Closeable {

  private static final Logger logger = Logger.getLogger(RuntimeMetrics.class.getName());

  private final AtomicBoolean isClosed = new AtomicBoolean();
  private final List<AutoCloseable> observables;

  RuntimeMetrics(List<AutoCloseable> observables) {
    this.observables = Collections.unmodifiableList(observables);
  }

  /**
   * Create and start {@link RuntimeMetrics}.
   *
   * <p>Listens for select JFR events, extracts data, and records to various metrics. Recording will
   * continue until {@link #close()} is called.
   *
   * @param openTelemetry the {@link OpenTelemetry} instance used to record telemetry
   */
  public static RuntimeMetrics create(OpenTelemetry openTelemetry) {
    return new RuntimeMetricsBuilder(openTelemetry).build();
  }

  /**
   * Create a builder for configuring {@link RuntimeMetrics}.
   *
   * @param openTelemetry the {@link OpenTelemetry} instance used to record telemetry
   */
  public static RuntimeMetricsBuilder builder(OpenTelemetry openTelemetry) {
    return new RuntimeMetricsBuilder(openTelemetry);
  }

  /** Stop recording JFR events. */
  @Override
  public void close() {
    if (!isClosed.compareAndSet(false, true)) {
      logger.log(Level.WARNING, "RuntimeMetrics is already closed");
      return;
    }

    JmxRuntimeMetricsUtil.closeObservers(observables);
  }
}
