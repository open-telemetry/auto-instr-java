/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.instrumenter.http;

import static java.util.logging.Level.FINE;

import com.google.auto.value.AutoValue;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.instrumentation.api.instrumenter.OperationListener;
import io.opentelemetry.instrumentation.api.instrumenter.OperationMetrics;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * {@link OperationListener} which keeps track of <a
 * href="https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/metrics/semantic_conventions/http-metrics.md#http-client">HTTP
 * client metrics</a>.
 */
public final class HttpClientMetrics implements OperationListener {

  private static final double NANOS_PER_MS = TimeUnit.MILLISECONDS.toNanos(1);

  private static final ContextKey<State> HTTP_CLIENT_REQUEST_METRICS_STATE =
      ContextKey.named("http-client-request-metrics-state");

  private static final Logger logger = Logger.getLogger(HttpClientMetrics.class.getName());

  /**
   * Returns a {@link OperationMetrics} which can be used to enable recording of {@link
   * HttpClientMetrics} on an {@link
   * io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder}.
   */
  public static OperationMetrics get() {
    return HttpClientMetrics::new;
  }

  private final DoubleHistogram duration;
  private final LongHistogram requestSize;
  private final LongHistogram responseSize;

  private HttpClientMetrics(Meter meter) {
    duration =
        meter
            .histogramBuilder("http.client.duration")
            .setUnit("ms")
            .setDescription("The duration of the outbound HTTP request")
            .build();
    requestSize =
        meter
            .histogramBuilder("http.client.request.size")
            .setUnit("By")
            .setDescription("The size of HTTP request messages")
            .ofLongs()
            .build();
    responseSize =
        meter
            .histogramBuilder("http.client.response.size")
            .setUnit("By")
            .setDescription("The size of HTTP response messages")
            .ofLongs()
            .build();
  }

  @Override
  public Context onStart(Context context, Attributes startAttributes, long startNanos) {
    return context.with(
        HTTP_CLIENT_REQUEST_METRICS_STATE,
        new AutoValue_HttpClientMetrics_State(startAttributes, startNanos));
  }

  @Override
  public void onEnd(Context context, Attributes endAttributes, long endNanos) {
    State state = context.get(HTTP_CLIENT_REQUEST_METRICS_STATE);
    if (state == null) {
      logger.log(
          FINE,
          "No state present when ending context {0}. Cannot record HTTP request metrics.",
          context);
      return;
    }
    Attributes attributes = mergeAttributes(state.startAttributes(), endAttributes);
    duration.record((endNanos - state.startTimeNanos()) / NANOS_PER_MS, attributes, context);
    Long requestLength = attributes.get(SemanticAttributes.HTTP_REQUEST_CONTENT_LENGTH);
    if (requestLength != null) {
      requestSize.record(requestLength, attributes);
    }
    Long responseLength = attributes.get(SemanticAttributes.HTTP_RESPONSE_CONTENT_LENGTH);
    if (responseLength != null) {
      responseSize.record(responseLength, attributes);
    }
  }

  private static Attributes mergeAttributes(Attributes attr1, Attributes attr2) {
    return Attributes.builder().putAll(attr1).putAll(attr2).build();
  }

  @AutoValue
  abstract static class State {

    abstract Attributes startAttributes();

    abstract long startTimeNanos();
  }
}
