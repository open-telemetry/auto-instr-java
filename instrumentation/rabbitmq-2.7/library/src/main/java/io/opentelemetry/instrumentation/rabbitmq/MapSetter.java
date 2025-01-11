/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.rabbitmq;

import io.opentelemetry.context.propagation.TextMapSetter;
import java.util.Map;

public enum MapSetter implements TextMapSetter<Map<String, Object>> {
  INSTANCE;

  @Override
  public void set(Map<String, Object> carrier, String key, String value) {
    carrier.put(key, value);
  }
}
