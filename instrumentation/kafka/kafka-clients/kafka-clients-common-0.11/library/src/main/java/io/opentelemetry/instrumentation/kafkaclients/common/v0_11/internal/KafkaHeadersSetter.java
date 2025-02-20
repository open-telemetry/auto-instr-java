/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal;

import io.opentelemetry.context.propagation.TextMapSetter;
import java.nio.charset.StandardCharsets;
import org.apache.kafka.common.header.Headers;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public enum KafkaHeadersSetter implements TextMapSetter<Headers> {
  INSTANCE;

  @Override
  public void set(Headers headers, String key, String value) {
    headers.remove(key).add(key, value.getBytes(StandardCharsets.UTF_8));
  }
}
