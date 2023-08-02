/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.thrift;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import javax.annotation.Nullable;

final class ThriftAttributesExtractor implements AttributesExtractor<ThriftRequest, Integer> {

  @Override
  public void onStart(AttributesBuilder attributes, Context parentContext, ThriftRequest request) {
    // Request attributes captured on request end.
  }

  @Override
  public void onEnd(
      AttributesBuilder attributes,
      Context context,
      ThriftRequest request,
      @Nullable Integer status,
      @Nullable Throwable error) {
    for (String key : request.args.keySet()) {
      String value = request.args.get(key);
      attributes.put(AttributeKey.stringArrayKey(key), value);
    }
  }
}
