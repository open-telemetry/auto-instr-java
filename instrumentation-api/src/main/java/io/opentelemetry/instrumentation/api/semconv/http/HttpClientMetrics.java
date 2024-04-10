/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.semconv.http;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.DoubleHistogramBuilder;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.OperationListener;
import io.opentelemetry.instrumentation.api.instrumenter.OperationMetrics;
import io.opentelemetry.instrumentation.api.internal.OperationMetricsUtil;
import java.util.concurrent.TimeUnit;

/**
 * {@link OperationListener} which keeps track of <a
 * href="https://github.com/open-telemetry/semantic-conventions/blob/v1.23.0/docs/http/http-metrics.md#http-client">HTTP
 * client metrics</a>.
 *
 * @since 2.0.0
 */
public final class HttpClientMetrics implements OperationListener {

  private static final double NANOS_PER_S = TimeUnit.SECONDS.toNanos(1);

  /**
   * Returns an {@link OperationMetrics} instance which can be used to enable recording of {@link
   * HttpClientMetrics}.
   *
   * @see InstrumenterBuilder#addOperationMetrics(OperationMetrics)
   */
  public static OperationMetrics get() {
    return OperationMetricsUtil.create("http client", HttpClientMetrics::new);
  }

  private final DoubleHistogram duration;

  private HttpClientMetrics(Meter meter) {
    DoubleHistogramBuilder stableDurationBuilder =
        meter
            .histogramBuilder("http.client.request.duration")
            .setUnit("s")
            .setDescription("Duration of HTTP client requests.")
            .setExplicitBucketBoundariesAdvice(HttpMetricsAdvice.DURATION_SECONDS_BUCKETS);
    HttpMetricsAdvice.applyClientDurationAdvice(stableDurationBuilder);
    duration = stableDurationBuilder.build();
  }

  @Override
  @SuppressWarnings("OtelCanIgnoreReturnValueSuggester")
  public Context onStart(Context context, Attributes startAttributes, long startNanos) {
    return context;
  }

  @Override
  public void onEnd(
      Context context, Attributes startAndEndAttributes, long startNanos, long endNanos) {
    duration.record((endNanos - startNanos) / NANOS_PER_S, startAndEndAttributes, context);
  }
}
