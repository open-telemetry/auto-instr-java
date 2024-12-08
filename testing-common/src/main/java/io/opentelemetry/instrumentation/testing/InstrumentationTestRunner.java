/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.testing;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static org.awaitility.Awaitility.await;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.testing.util.TelemetryDataUtil;
import io.opentelemetry.instrumentation.testing.util.ThrowingRunnable;
import io.opentelemetry.instrumentation.testing.util.ThrowingSupplier;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.metrics.data.MetricData;
import io.opentelemetry.sdk.testing.assertj.MetricAssert;
import io.opentelemetry.sdk.testing.assertj.TraceAssert;
import io.opentelemetry.sdk.testing.assertj.TracesAssert;
import io.opentelemetry.sdk.trace.data.SpanData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.assertj.core.api.ListAssert;

/**
 * This interface defines a common set of operations for interaction with OpenTelemetry SDK and
 * traces & metrics exporters.
 *
 * @see LibraryTestRunner
 * @see AgentTestRunner
 */
public abstract class InstrumentationTestRunner {

  private final TestInstrumenters testInstrumenters;

  protected InstrumentationTestRunner(OpenTelemetry openTelemetry) {
    testInstrumenters = new TestInstrumenters(openTelemetry);
  }

  public abstract void beforeTestClass();

  public abstract void afterTestClass();

  public abstract void clearAllExportedData();

  public abstract OpenTelemetry getOpenTelemetry();

  public abstract List<SpanData> getExportedSpans();

  public abstract List<MetricData> getExportedMetrics();

  public abstract List<LogRecordData> getExportedLogRecords();

  public abstract boolean forceFlushCalled();

  /** Return a list of all captured traces, where each trace is a sorted list of spans. */
  public final List<List<SpanData>> traces() {
    return TelemetryDataUtil.groupTraces(getExportedSpans());
  }

  public final List<List<SpanData>> waitForTraces(int numberOfTraces) {
    try {
      return TelemetryDataUtil.waitForTraces(
          this::getExportedSpans, numberOfTraces, 20, TimeUnit.SECONDS);
    } catch (TimeoutException | InterruptedException e) {
      throw new AssertionError("Error waiting for " + numberOfTraces + " traces", e);
    }
  }

  @SafeVarargs
  @SuppressWarnings("varargs")
  public final void waitAndAssertSortedTraces(
      Comparator<List<SpanData>> traceComparator, Consumer<TraceAssert>... assertions) {
    waitAndAssertTraces(traceComparator, Arrays.asList(assertions), true);
  }

  public final void waitAndAssertSortedTraces(
      Comparator<List<SpanData>> traceComparator,
      Iterable<? extends Consumer<TraceAssert>> assertions) {
    waitAndAssertTraces(traceComparator, assertions, true);
  }

  @SafeVarargs
  @SuppressWarnings("varargs")
  public final void waitAndAssertTracesWithoutScopeVersionVerification(
      Consumer<TraceAssert>... assertions) {
    waitAndAssertTracesWithoutScopeVersionVerification(Arrays.asList(assertions));
  }

  public final <T extends Consumer<TraceAssert>>
      void waitAndAssertTracesWithoutScopeVersionVerification(Iterable<T> assertions) {
    waitAndAssertTraces(null, assertions, false);
  }

  @SafeVarargs
  @SuppressWarnings("varargs")
  public final void waitAndAssertTraces(Consumer<TraceAssert>... assertions) {
    waitAndAssertTraces(Arrays.asList(assertions));
  }

  public final <T extends Consumer<TraceAssert>> void waitAndAssertTraces(Iterable<T> assertions) {
    waitAndAssertTraces(null, assertions, true);
  }

  private <T extends Consumer<TraceAssert>> void waitAndAssertTraces(
      @Nullable Comparator<List<SpanData>> traceComparator,
      Iterable<T> assertions,
      boolean verifyScopeVersion) {
    List<T> assertionsList = new ArrayList<>();
    assertions.forEach(assertionsList::add);

    doAssertTraces(traceComparator, assertionsList, verifyScopeVersion);
  }

  private <T extends Consumer<TraceAssert>> void doAssertTraces(
      @Nullable Comparator<List<SpanData>> traceComparator,
      List<T> assertionsList,
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

  /**
   * Waits for the assertion applied to all metrics of the given instrumentation and metric name to
   * pass.
   */
  public final void waitAndAssertMetrics(
      String instrumentationName, String metricName, Consumer<ListAssert<MetricData>> assertion) {
    await()
        .untilAsserted(
            () ->
                assertion.accept(
                    assertThat(getExportedMetrics())
                        .filteredOn(
                            data ->
                                data.getInstrumentationScopeInfo()
                                        .getName()
                                        .equals(instrumentationName)
                                    && data.getName().equals(metricName))));
  }

  @SafeVarargs
  public final void waitAndAssertMetrics(
      String instrumentationName, Consumer<MetricAssert>... assertions) {
    await()
        .untilAsserted(
            () -> {
              Collection<MetricData> metrics = instrumentationMetrics(instrumentationName);
              assertThat(metrics).isNotEmpty();
              for (Consumer<MetricAssert> assertion : assertions) {
                assertThat(metrics).anySatisfy(metric -> assertion.accept(assertThat(metric)));
              }
            });
  }

  private List<MetricData> instrumentationMetrics(String instrumentationName) {
    return getExportedMetrics().stream()
        .filter(m -> m.getInstrumentationScopeInfo().getName().equals(instrumentationName))
        .collect(Collectors.toList());
  }

  /**
   * Runs the provided {@code callback} inside the scope of an INTERNAL span with name {@code
   * spanName}.
   */
  public final <E extends Exception> void runWithSpan(String spanName, ThrowingRunnable<E> callback)
      throws E {
    runWithSpan(
        spanName,
        () -> {
          callback.run();
          return null;
        });
  }

  /**
   * Runs the provided {@code callback} inside the scope of an INTERNAL span with name {@code
   * spanName}.
   */
  public final <T, E extends Throwable> T runWithSpan(
      String spanName, ThrowingSupplier<T, E> callback) throws E {
    return testInstrumenters.runWithSpan(spanName, callback);
  }

  /**
   * Runs the provided {@code callback} inside the scope of an HTTP CLIENT span with name {@code
   * spanName}.
   */
  public final <E extends Throwable> void runWithHttpClientSpan(
      String spanName, ThrowingRunnable<E> callback) throws E {
    runWithHttpClientSpan(
        spanName,
        () -> {
          callback.run();
          return null;
        });
  }

  /**
   * Runs the provided {@code callback} inside the scope of an HTTP CLIENT span with name {@code
   * spanName}.
   */
  public final <T, E extends Throwable> T runWithHttpClientSpan(
      String spanName, ThrowingSupplier<T, E> callback) throws E {
    return testInstrumenters.runWithHttpClientSpan(spanName, callback);
  }

  /**
   * Runs the provided {@code callback} inside the scope of an HTTP SERVER span with name {@code
   * spanName}.
   */
  public final <E extends Throwable> void runWithHttpServerSpan(ThrowingRunnable<E> callback)
      throws E {
    runWithHttpServerSpan(
        () -> {
          callback.run();
          return null;
        });
  }

  /**
   * Runs the provided {@code callback} inside the scope of an HTTP SERVER span with name {@code
   * spanName}.
   */
  public final <T, E extends Throwable> T runWithHttpServerSpan(ThrowingSupplier<T, E> callback)
      throws E {
    return testInstrumenters.runWithHttpServerSpan(callback);
  }

  /** Runs the provided {@code callback} inside the scope of a non-recording span. */
  public final <T, E extends Throwable> T runWithNonRecordingSpan(ThrowingSupplier<T, E> callback)
      throws E {
    return testInstrumenters.runWithNonRecordingSpan(callback);
  }
}
