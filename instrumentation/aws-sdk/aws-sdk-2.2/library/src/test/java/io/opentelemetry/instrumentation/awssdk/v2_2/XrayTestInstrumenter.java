/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v2_2;

import static org.awaitility.Awaitility.await;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.contrib.awsxray.propagator.AwsXrayPropagator;
import io.opentelemetry.instrumentation.testing.util.TelemetryDataUtil;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.data.AggregationTemporality;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.opentelemetry.sdk.testing.assertj.TraceAssert;
import io.opentelemetry.sdk.testing.assertj.TracesAssert;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricExporter;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.SpanProcessor;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import javax.annotation.Nullable;
import org.awaitility.core.ConditionTimeoutException;
import org.junit.jupiter.api.AfterEach;

public class XrayTestInstrumenter {
  private static final OpenTelemetrySdk openTelemetry;
  private static final InMemorySpanExporter testSpanExporter;

  static {
    testSpanExporter = InMemorySpanExporter.create();
    InMemoryMetricExporter testMetricExporter =
        InMemoryMetricExporter.create(AggregationTemporality.DELTA);

    MetricReader metricReader =
        PeriodicMetricReader.builder(testMetricExporter)
            // Set really long interval. We'll call forceFlush when we need the metrics
            // instead of collecting them periodically.
            .setInterval(Duration.ofNanos(Long.MAX_VALUE))
            .build();

    openTelemetry =
        OpenTelemetrySdk.builder()
            .setTracerProvider(
                SdkTracerProvider.builder()
                    .addSpanProcessor(new FlushTrackingSpanProcessor())
                    .addSpanProcessor(SimpleSpanProcessor.create(testSpanExporter))
                    .build())
            .setMeterProvider(SdkMeterProvider.builder().registerMetricReader(metricReader).build())
            .setPropagators(ContextPropagators.create(AwsXrayPropagator.getInstance()))
            .buildAndRegisterGlobal();
  }

  @AfterEach
  public void resetTests() {
    testSpanExporter.reset();
  }

  protected OpenTelemetrySdk getOpenTelemetry() {
    return openTelemetry;
  }

  private static class FlushTrackingSpanProcessor implements SpanProcessor {
    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {}

    @Override
    public boolean isStartRequired() {
      return false;
    }

    @Override
    public void onEnd(ReadableSpan span) {}

    @Override
    public boolean isEndRequired() {
      return false;
    }

    @Override
    public CompletableResultCode forceFlush() {
      return CompletableResultCode.ofSuccess();
    }
  }

  @SafeVarargs
  @SuppressWarnings("varargs")
  public final void waitAndAssertTraces(Consumer<TraceAssert>... assertions) {
    waitAndAssertTraces(null, Arrays.asList(assertions), true);
  }

  private <T extends Consumer<TraceAssert>> void waitAndAssertTraces(
      @Nullable Comparator<List<SpanData>> traceComparator,
      Iterable<T> assertions,
      boolean verifyScopeVersion) {
    List<Consumer<TraceAssert>> assertionsList = new ArrayList<>();
    assertions.forEach(assertionsList::add);

    try {
      await()
          .untilAsserted(() -> doAssertTraces(traceComparator, assertionsList, verifyScopeVersion));
    } catch (ConditionTimeoutException e) {
      // Don't throw this failure since the stack is the awaitility thread, causing confusion.
      // Instead, just assert one more time on the test thread, which will fail with a better stack
      // trace.
      // TODO(anuraaga): There is probably a better way to do this.
      doAssertTraces(traceComparator, assertionsList, verifyScopeVersion);
    }
  }

  private void doAssertTraces(
      @Nullable Comparator<List<SpanData>> traceComparator,
      List<Consumer<TraceAssert>> assertionsList,
      boolean verifyScopeVersion) {
    List<List<SpanData>> traces = waitForTraces(assertionsList.size());
    if (verifyScopeVersion) {
      TelemetryDataUtil.assertScopeVersion(traces);
    }
    if (traceComparator != null) {
      traces.sort(traceComparator);
    }
    TracesAssert.assertThat(traces).hasTracesSatisfyingExactly(assertionsList);
  }

  public final List<List<SpanData>> waitForTraces(int numberOfTraces) {
    try {
      return TelemetryDataUtil.waitForTraces(
          this::getExportedSpans, numberOfTraces, 20, TimeUnit.SECONDS);
    } catch (TimeoutException | InterruptedException e) {
      throw new AssertionError("Error waiting for " + numberOfTraces + " traces", e);
    }
  }

  public List<SpanData> getExportedSpans() {
    return testSpanExporter.getFinishedSpanItems();
  }
}
