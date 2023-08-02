/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.thrift;

import io.opentelemetry.context.propagation.TextMapSetter;
import javax.annotation.Nullable;

public enum ThriftHeaderSetter implements TextMapSetter<ThriftRequest> {
  INSTANCE;

  @Override
  public void set(@Nullable ThriftRequest request, String key, String value) {
    if (request != null) {
      request.setAttachment(key, value);
    }
  }
}
