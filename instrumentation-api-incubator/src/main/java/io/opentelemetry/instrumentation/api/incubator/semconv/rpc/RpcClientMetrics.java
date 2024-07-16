/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.incubator.semconv.rpc;

import static java.util.logging.Level.FINE;

import com.google.auto.value.AutoValue;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.DoubleHistogramBuilder;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.LongHistogramBuilder;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.instrumentation.api.instrumenter.OperationListener;
import io.opentelemetry.instrumentation.api.instrumenter.OperationMetrics;
import io.opentelemetry.instrumentation.api.internal.OperationMetricsUtil;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

/**
 * {@link OperationListener} which keeps track of <a
 * href="https://github.com/open-telemetry/semantic-conventions/blob/main/docs/rpc/rpc-metrics.md#rpc-client">RPC
 * client metrics</a>.
 */
public final class RpcClientMetrics implements OperationListener {

  private static final double NANOS_PER_MS = TimeUnit.MILLISECONDS.toNanos(1);

  private static final ContextKey<RpcClientMetrics.State> RPC_CLIENT_REQUEST_METRICS_STATE =
      ContextKey.named("rpc-client-request-metrics-state");

  private static final Logger logger = Logger.getLogger(RpcClientMetrics.class.getName());

  private final DoubleHistogram clientDurationHistogram;
  private final LongHistogram clientRequestSize;
  private final LongHistogram clientResponseSize;

  private RpcClientMetrics(Meter meter) {
    DoubleHistogramBuilder durationBuilder =
        meter
            .histogramBuilder("rpc.client.duration")
            .setDescription("The duration of an outbound RPC invocation")
            .setUnit("ms");
    RpcMetricsAdvice.applyClientDurationAdvice(durationBuilder);
    clientDurationHistogram = durationBuilder.build();

    LongHistogramBuilder requestSizeBuilder =
        meter
            .histogramBuilder("rpc.client.request.size")
            .setUnit("By")
            .setDescription("Measures the size of RPC request messages (uncompressed).")
            .ofLongs();
    RpcMetricsAdvice.applyClientRequestSizeAdvice(requestSizeBuilder);
    clientRequestSize = requestSizeBuilder.build();

    LongHistogramBuilder responseSizeBuilder =
        meter
            .histogramBuilder("rpc.client.response.size")
            .setUnit("By")
            .setDescription("Measures the size of RPC response messages (uncompressed).")
            .ofLongs();
    RpcMetricsAdvice.applyClientRequestSizeAdvice(responseSizeBuilder);
    clientResponseSize = responseSizeBuilder.build();
  }

  /**
   * Returns a {@link OperationMetrics} which can be used to enable recording of {@link
   * RpcClientMetrics} on an {@link
   * io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder}.
   */
  public static OperationMetrics get() {
    return OperationMetricsUtil.create("rpc client", RpcClientMetrics::new);
  }

  @Override
  public Context onStart(Context context, Attributes startAttributes, long startNanos) {
    return context.with(
        RPC_CLIENT_REQUEST_METRICS_STATE,
        new AutoValue_RpcClientMetrics_State(startAttributes, startNanos));
  }

  @Override
  public void onEnd(Context context, Attributes endAttributes, long endNanos) {
    State state = context.get(RPC_CLIENT_REQUEST_METRICS_STATE);
    if (state == null) {
      logger.log(
          FINE,
          "No state present when ending context {0}. Cannot record RPC request metrics.",
          context);
      return;
    }
    Attributes attributes = state.startAttributes().toBuilder().putAll(endAttributes).build();
    clientDurationHistogram.record(
        (endNanos - state.startTimeNanos()) / NANOS_PER_MS, attributes, context);

    Long rpcClientRequestBodySize =
        RpcMessageBodySizeUtil.getRpcClientRequestBodySize(endAttributes, state.startAttributes());
    if (rpcClientRequestBodySize != null && rpcClientRequestBodySize > 0) {
      clientRequestSize.record(rpcClientRequestBodySize, attributes, context);
    }

    Long rpcClientResponseBodySize =
        RpcMessageBodySizeUtil.getRpcClientResponseBodySize(endAttributes, state.startAttributes());
    if (rpcClientResponseBodySize != null && rpcClientResponseBodySize > 0) {
      clientResponseSize.record(rpcClientResponseBodySize, attributes, context);
    }
  }

  @AutoValue
  abstract static class State {

    abstract Attributes startAttributes();

    abstract long startTimeNanos();
  }
}
