/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.rabbitmq;

import static java.util.Collections.emptySet;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.GetResponse;
import io.opentelemetry.context.propagation.TextMapGetter;
import java.util.Map;
import javax.annotation.Nullable;

enum ReceiveRequestTextMapGetter implements TextMapGetter<ReceiveRequest> {
  INSTANCE;

  @Override
  public Iterable<String> keys(ReceiveRequest carrier) {
    Map<String, Object> headers = getHeaders(carrier);
    return headers == null ? emptySet() : headers.keySet();
  }

  @Nullable
  @Override
  public String get(@Nullable ReceiveRequest carrier, String key) {
    Map<String, Object> headers = getHeaders(carrier);
    if (headers == null) {
      return null;
    }
    Object value = headers.get(key);
    return value == null ? null : value.toString();
  }

  @Nullable
  private static Map<String, Object> getHeaders(@Nullable ReceiveRequest carrier) {
    if (carrier == null) {
      return null;
    }
    GetResponse response = carrier.getResponse();
    if (response == null) {
      return null;
    }
    AMQP.BasicProperties props = response.getProps();
    if (props == null) {
      return null;
    }
    return props.getHeaders();
  }
}
