/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.spring.pulsar.v1_0;

import io.opentelemetry.context.propagation.TextMapGetter;
import javax.annotation.Nullable;
import org.apache.pulsar.client.api.Message;

enum MessageHeaderGetter implements TextMapGetter<Message<?>> {
  INSTANCE;

  @Override
  public Iterable<String> keys(Message<?> carrier) {
    return carrier.getProperties().keySet();
  }

  @Nullable
  @Override
  public String get(@Nullable Message<?> carrier, String key) {
    return carrier == null ? null : carrier.getProperties().get(key);
  }
}
