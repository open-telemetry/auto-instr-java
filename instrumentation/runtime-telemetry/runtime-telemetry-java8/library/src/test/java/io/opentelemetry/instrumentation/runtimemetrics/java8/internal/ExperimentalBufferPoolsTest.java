/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.runtimemetrics.java8.internal;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.opentelemetry.instrumentation.runtimemetrics.java8.ScopeUtil.EXPECTED_SCOPE;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static java.util.Collections.singletonList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.ObservableLongMeasurement;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.LibraryInstrumentationExtension;
import java.lang.management.BufferPoolMXBean;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExperimentalBufferPoolsTest {

  @RegisterExtension
  static final InstrumentationExtension testing = LibraryInstrumentationExtension.create();

  @Spy private ObservableLongMeasurement measurement;
  @Mock private BufferPoolMXBean bufferPoolBean;
  private List<BufferPoolMXBean> beans;

  @BeforeEach
  void setup() {
    when(bufferPoolBean.getName()).thenReturn("buffer_pool_1");
    beans = singletonList(bufferPoolBean);
  }

  @Test
  void registerObservers() {
    when(bufferPoolBean.getMemoryUsed()).thenReturn(10L);
    when(bufferPoolBean.getTotalCapacity()).thenReturn(11L);
    when(bufferPoolBean.getCount()).thenReturn(12L);

    ExperimentalBufferPools.registerObservers(testing.getOpenTelemetry(), beans);

    testing.waitAndAssertMetrics(
        "io.opentelemetry.runtime-telemetry-java8",
        "jvm.buffer.memory.used",
        metrics ->
            metrics.anySatisfy(
                metricData ->
                    assertThat(metricData)
                        .hasInstrumentationScope(EXPECTED_SCOPE)
                        .hasDescription("Measure of memory used by buffers.")
                        .hasUnit("By")
                        .hasLongSumSatisfying(
                            sum ->
                                sum.hasPointsSatisfying(
                                    point ->
                                        point
                                            .hasValue(10)
                                            .hasAttribute(
                                                stringKey("jvm.buffer.pool.name"),
                                                "buffer_pool_1")))));
    testing.waitAndAssertMetrics(
        "io.opentelemetry.runtime-telemetry-java8",
        "jvm.buffer.memory.limit",
        metrics ->
            metrics.anySatisfy(
                metricData ->
                    assertThat(metricData)
                        .hasInstrumentationScope(EXPECTED_SCOPE)
                        .hasDescription("Measure of total memory capacity of buffers.")
                        .hasUnit("By")
                        .hasLongSumSatisfying(
                            sum ->
                                sum.hasPointsSatisfying(
                                    point ->
                                        point
                                            .hasValue(11)
                                            .hasAttribute(
                                                stringKey("jvm.buffer.pool.name"),
                                                "buffer_pool_1")))));
    testing.waitAndAssertMetrics(
        "io.opentelemetry.runtime-telemetry-java8",
        "jvm.buffer.count",
        metrics ->
            metrics.anySatisfy(
                metricData ->
                    assertThat(metricData)
                        .hasInstrumentationScope(EXPECTED_SCOPE)
                        .hasDescription("Number of buffers in the pool.")
                        .hasUnit("{buffer}")
                        .hasLongSumSatisfying(
                            sum ->
                                sum.hasPointsSatisfying(
                                    point ->
                                        point
                                            .hasValue(12)
                                            .hasAttribute(
                                                stringKey("jvm.buffer.pool.name"),
                                                "buffer_pool_1")))));
  }

  @Test
  void callback_Records() {
    when(bufferPoolBean.getMemoryUsed()).thenReturn(1L);
    Consumer<ObservableLongMeasurement> callback =
        ExperimentalBufferPools.callback(beans, BufferPoolMXBean::getMemoryUsed);
    callback.accept(measurement);
    verify(measurement)
        .record(1, Attributes.builder().put("jvm.buffer.pool.name", "buffer_pool_1").build());
  }

  @Test
  void callback_SkipRecord() {
    when(bufferPoolBean.getMemoryUsed()).thenReturn(-1L);
    Consumer<ObservableLongMeasurement> callback =
        ExperimentalBufferPools.callback(beans, BufferPoolMXBean::getMemoryUsed);
    callback.accept(measurement);
    verify(measurement, never()).record(eq(-1), any());
  }
}
