/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.runtimemetrics.java8;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.incubator.config.internal.InstrumentationConfig;
import io.opentelemetry.instrumentation.runtimemetrics.java8.internal.JmxRuntimeMetricsFactory;
import java.util.List;
import java.util.function.Consumer;

/** Builder for {@link RuntimeMetrics}. */
public final class RuntimeMetricsBuilder {

  private final OpenTelemetry openTelemetry;

  private boolean enableExperimentalJmxTelemetry = false;
  private Consumer<Runnable> shutdownHook =
      runnable -> {
        Runtime.getRuntime().addShutdownHook(new Thread(runnable));
      };

  RuntimeMetricsBuilder(OpenTelemetry openTelemetry) {
    this.openTelemetry = openTelemetry;
  }

  /** Enable all JMX telemetry collection. */
  @CanIgnoreReturnValue
  public RuntimeMetricsBuilder enableExperimentalJmxTelemetry() {
    enableExperimentalJmxTelemetry = true;
    return this;
  }

  /** Build and start an {@link RuntimeMetrics} with the config from this builder. */
  public RuntimeMetrics build() {
    List<AutoCloseable> observables =
        JmxRuntimeMetricsFactory.buildObservables(openTelemetry, enableExperimentalJmxTelemetry);
    return new RuntimeMetrics(observables);
  }

  /** Set a custom shutdown hook for the {@link RuntimeMetrics}. */
  @CanIgnoreReturnValue
  public RuntimeMetricsBuilder setShutdownHook(Consumer<Runnable> shutdownHook) {
    this.shutdownHook = shutdownHook;
    return this;
  }

  public void startFromInstrumentationConfig(InstrumentationConfig config) {
    /*
    By default, don't use any JFR metrics. May change this once semantic conventions are updated.
    If enabled, default to only the metrics not already covered by runtime-telemetry-java8
    */
    boolean defaultEnabled = config.getBoolean("otel.instrumentation.common.default-enabled", true);
    if (!config.getBoolean("otel.instrumentation.runtime-telemetry.enabled", defaultEnabled)) {
      // nothing is enabled
      return;
    }

    if (config.getBoolean(
        "otel.instrumentation.runtime-telemetry.emit-experimental-telemetry", false)) {
      this.enableExperimentalJmxTelemetry();
    }

    RuntimeMetrics runtimeMetrics = this.build();
    shutdownHook.accept(runtimeMetrics::close);
  }
}
