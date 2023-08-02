/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.thrift;

import io.opentelemetry.instrumentation.api.instrumenter.SpanStatusBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.SpanStatusExtractor;
import javax.annotation.Nullable;

final class ThriftSpanStatusExtractor implements SpanStatusExtractor<ThriftRequest, Integer> {
  @Override
  public void extract(
      SpanStatusBuilder spanStatusBuilder,
      ThriftRequest request,
      @Nullable Integer status,
      @Nullable Throwable error) {
    SpanStatusExtractor.getDefault().extract(spanStatusBuilder, request, status, error);
  }
}
