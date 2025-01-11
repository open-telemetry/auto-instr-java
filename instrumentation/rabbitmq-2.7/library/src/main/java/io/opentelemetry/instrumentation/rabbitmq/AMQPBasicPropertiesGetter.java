/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.rabbitmq;

import com.rabbitmq.client.AMQP;
import io.opentelemetry.context.propagation.TextMapGetter;
import java.util.Collections;

enum AMQPBasicPropertiesGetter implements TextMapGetter<AMQP.BasicProperties> {
  INSTANCE;

  @Override
  public Iterable<String> keys(AMQP.BasicProperties carrier) {
    if (carrier == null) {
      return Collections.emptyList();
    }
    return carrier.getHeaders().keySet();
  }

  @Override
  public String get(AMQP.BasicProperties carrier, String key) {
    if (carrier == null) {
      return null;
    }

    Object obj = carrier.getHeaders().get(key);
    return obj == null ? null : obj.toString();
  }
}
