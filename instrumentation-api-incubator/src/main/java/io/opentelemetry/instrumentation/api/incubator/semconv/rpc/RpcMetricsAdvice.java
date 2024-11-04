/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.incubator.semconv.rpc;

import static java.util.Arrays.asList;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.incubator.metrics.ExtendedDoubleHistogramBuilder;
import io.opentelemetry.api.incubator.metrics.ExtendedLongHistogramBuilder;
import io.opentelemetry.api.metrics.DoubleHistogramBuilder;
import io.opentelemetry.api.metrics.LongHistogramBuilder;
import io.opentelemetry.semconv.NetworkAttributes;
import io.opentelemetry.semconv.ServerAttributes;
import java.util.List;

final class RpcMetricsAdvice {

  // copied from RpcIncubatingAttributes
  private static final AttributeKey<Long> RPC_GRPC_STATUS_CODE =
      AttributeKey.longKey("rpc.grpc.status_code");
  private static final List<AttributeKey<?>> RPC_METRICS_ATTRIBUTE_KEYS =
      asList(
          RpcCommonAttributesExtractor.RPC_SYSTEM,
          RpcCommonAttributesExtractor.RPC_SERVICE,
          RpcCommonAttributesExtractor.RPC_METHOD,
          RPC_GRPC_STATUS_CODE,
          NetworkAttributes.NETWORK_TYPE,
          NetworkAttributes.NETWORK_TRANSPORT,
          ServerAttributes.SERVER_ADDRESS,
          ServerAttributes.SERVER_PORT);

  static void applyClientDurationAdvice(DoubleHistogramBuilder builder) {
    if (!(builder instanceof ExtendedDoubleHistogramBuilder)) {
      return;
    }
    // the list of recommended metrics attributes is from
    // https://github.com/open-telemetry/semantic-conventions/blob/main/docs/rpc/rpc-metrics.md
    ((ExtendedDoubleHistogramBuilder) builder).setAttributesAdvice(RPC_METRICS_ATTRIBUTE_KEYS);
  }

  static void applyServerDurationAdvice(DoubleHistogramBuilder builder) {
    if (!(builder instanceof ExtendedDoubleHistogramBuilder)) {
      return;
    }
    // the list of recommended metrics attributes is from
    // https://github.com/open-telemetry/semantic-conventions/blob/main/docs/rpc/rpc-metrics.md
    ((ExtendedDoubleHistogramBuilder) builder).setAttributesAdvice(RPC_METRICS_ATTRIBUTE_KEYS);
  }

  static void applyClientRequestSizeAdvice(LongHistogramBuilder builder) {
    if (!(builder instanceof ExtendedLongHistogramBuilder)) {
      return;
    }
    // the list of recommended metrics attributes is from
    // https://github.com/open-telemetry/semantic-conventions/blob/main/docs/rpc/rpc-metrics.md
    ((ExtendedLongHistogramBuilder) builder).setAttributesAdvice(RPC_METRICS_ATTRIBUTE_KEYS);
  }

  static void applyServerRequestSizeAdvice(LongHistogramBuilder builder) {
    if (!(builder instanceof ExtendedLongHistogramBuilder)) {
      return;
    }
    // the list of recommended metrics attributes is from
    // https://github.com/open-telemetry/semantic-conventions/blob/main/docs/rpc/rpc-metrics.md
    ((ExtendedLongHistogramBuilder) builder).setAttributesAdvice(RPC_METRICS_ATTRIBUTE_KEYS);
  }

  private RpcMetricsAdvice() {}
}
