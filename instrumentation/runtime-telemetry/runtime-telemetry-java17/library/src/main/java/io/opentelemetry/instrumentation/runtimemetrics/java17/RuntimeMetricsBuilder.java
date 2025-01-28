/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.runtimemetrics.java17;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.incubator.config.internal.InstrumentationConfig;
import io.opentelemetry.instrumentation.runtimemetrics.java8.internal.JmxRuntimeMetricsFactory;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.function.Consumer;
import javax.annotation.Nullable;

/** Builder for {@link RuntimeMetrics}. */
public final class RuntimeMetricsBuilder {

  private final OpenTelemetry openTelemetry;
  // Visible for testing
  final EnumMap<JfrFeature, Boolean> enabledFeatureMap;

  private boolean disableJmx = false;
  private boolean enableExperimentalJmxTelemetry = false;
  private Consumer<Runnable> shutdownHook =
      runnable -> {
        Runtime.getRuntime().addShutdownHook(new Thread(runnable));
      };

  RuntimeMetricsBuilder(OpenTelemetry openTelemetry) {
    this.openTelemetry = openTelemetry;
    enabledFeatureMap = new EnumMap<>(JfrFeature.class);
    for (JfrFeature feature : JfrFeature.values()) {
      enabledFeatureMap.put(feature, feature.isDefaultEnabled());
    }
  }

  /** Enable telemetry collection for all {@link JfrFeature}s. */
  @CanIgnoreReturnValue
  public RuntimeMetricsBuilder enableAllFeatures() {
    Arrays.stream(JfrFeature.values()).forEach(this::enableFeature);
    return this;
  }

  /** Enable telemetry collection associated with the {@link JfrFeature}. */
  @CanIgnoreReturnValue
  public RuntimeMetricsBuilder enableFeature(JfrFeature feature) {
    enabledFeatureMap.put(feature, true);
    return this;
  }

  /** Disable telemetry collection for all {@link JfrFeature}s. */
  @CanIgnoreReturnValue
  public RuntimeMetricsBuilder disableAllFeatures() {
    Arrays.stream(JfrFeature.values()).forEach(this::disableFeature);
    return this;
  }

  /** Disable telemetry collection for all metrics. */
  @CanIgnoreReturnValue
  public RuntimeMetricsBuilder disableAllMetrics() {
    disableAllFeatures();
    disableAllJmx();
    return this;
  }

  /** Disable telemetry collection associated with the {@link JfrFeature}. */
  @CanIgnoreReturnValue
  public RuntimeMetricsBuilder disableFeature(JfrFeature feature) {
    enabledFeatureMap.put(feature, false);
    return this;
  }

  /** Disable all JMX telemetry collection. */
  @CanIgnoreReturnValue
  public RuntimeMetricsBuilder disableAllJmx() {
    disableJmx = true;
    return this;
  }

  /** Enable experimental JMX telemetry collection. */
  @CanIgnoreReturnValue
  public RuntimeMetricsBuilder enableExperimentalJmxTelemetry() {
    enableExperimentalJmxTelemetry = true;
    return this;
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
    if (config.getBoolean("otel.instrumentation.runtime-telemetry-java17.enable-all", false)) {
      this.enableAllFeatures();
    } else if (config.getBoolean("otel.instrumentation.runtime-telemetry-java17.enabled", false)) {
      // default configuration
    } else if (config.getBoolean(
        "otel.instrumentation.runtime-telemetry.enabled", defaultEnabled)) {
      // This only uses metrics gathered by JMX
      this.disableAllFeatures();
    } else {
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

  /** Build and start an {@link RuntimeMetrics} with the config from this builder. */
  public RuntimeMetrics build() {
    List<AutoCloseable> observables =
        disableJmx
            ? List.of()
            : JmxRuntimeMetricsFactory.buildObservables(
                openTelemetry, enableExperimentalJmxTelemetry);
    RuntimeMetrics.JfrRuntimeMetrics jfrRuntimeMetrics = buildJfrMetrics();
    return new RuntimeMetrics(openTelemetry, observables, jfrRuntimeMetrics);
  }

  @Nullable
  private RuntimeMetrics.JfrRuntimeMetrics buildJfrMetrics() {
    if (enabledFeatureMap.values().stream().noneMatch(isEnabled -> isEnabled)) {
      return null;
    }
    return RuntimeMetrics.JfrRuntimeMetrics.build(openTelemetry, enabledFeatureMap::get);
  }
}
