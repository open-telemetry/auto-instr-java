/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.netty.common.v4_0.internal.client;

import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.instrumentation.netty.common.v4_0.HttpRequestAndChannel;

enum HttpRequestHeadersSetter implements TextMapSetter<HttpRequestAndChannel> {
  INSTANCE;

  @Override
  public void set(HttpRequestAndChannel requestAndChannel, String key, String value) {
    requestAndChannel.request().headers().set(key, value);
  }
}
