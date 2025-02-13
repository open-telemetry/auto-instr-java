/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.runtimemetrics.java8.internal;

import static io.opentelemetry.instrumentation.runtimemetrics.java8.ScopeUtil.EXPECTED_SCOPE;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;

import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.LibraryInstrumentationExtension;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class ExperimentalFileDescriptorTest {
  @RegisterExtension
  static final InstrumentationExtension testing = LibraryInstrumentationExtension.create();

  @Test
  void registerObservers() {
    Supplier<Long> openFileDescriptor = () -> 10L;
    Supplier<Long> maxFileDescriptor = () -> 10000L;

    ExperimentalFileDescriptor.registerObservers(
        testing.getOpenTelemetry(), openFileDescriptor, maxFileDescriptor);

    testing.waitAndAssertMetrics(
        "io.opentelemetry.runtime-telemetry-java8",
        "jvm.open_file_descriptor.count",
        metrics ->
            metrics.anySatisfy(
                metricData ->
                    assertThat(metricData)
                        .hasInstrumentationScope(EXPECTED_SCOPE)
                        .hasDescription("Number of file descriptors in use by the process.")
                        .hasUnit("{count}")
                        .hasLongSumSatisfying(
                            sum -> sum.hasPointsSatisfying(point -> point.hasValue(10L)))));
    testing.waitAndAssertMetrics(
        "io.opentelemetry.runtime-telemetry-java8",
        "jvm.open_file_descriptor.limit",
        metrics ->
            metrics.anySatisfy(
                metricData ->
                    assertThat(metricData)
                        .hasInstrumentationScope(EXPECTED_SCOPE)
                        .hasDescription("Measure of max file descriptors.")
                        .hasUnit("{count}")
                        .hasLongSumSatisfying(
                            sum -> sum.hasPointsSatisfying(point -> point.hasValue(10000L)))));
  }
}
