/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.thrift;

import io.opentelemetry.context.propagation.TextMapGetter;
import javax.annotation.Nullable;

public enum ThriftHeaderGetter implements TextMapGetter<ThriftRequest> {
  INSTANCE;

  @Override
  public Iterable<String> keys(ThriftRequest request) {
    return request.getAttachments().keySet();
  }

  @Override
  @Nullable
  public String get(@Nullable ThriftRequest request, String key) {
    if (request == null) {
      return null;
    }
    return request.getAttachment(key);
  }
}
