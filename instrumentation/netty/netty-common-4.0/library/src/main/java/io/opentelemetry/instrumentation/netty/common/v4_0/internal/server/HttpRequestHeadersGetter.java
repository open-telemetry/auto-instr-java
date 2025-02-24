/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.netty.common.v4_0.internal.server;

import io.opentelemetry.context.propagation.internal.ExtendedTextMapGetter;
import io.opentelemetry.instrumentation.netty.common.v4_0.HttpRequestAndChannel;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import javax.annotation.Nullable;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public enum HttpRequestHeadersGetter implements ExtendedTextMapGetter<HttpRequestAndChannel> {
  INSTANCE;

  @Override
  public Iterable<String> keys(HttpRequestAndChannel carrier) {
    return carrier.request().headers().names();
  }

  @Nullable
  @Override
  public String get(@Nullable HttpRequestAndChannel carrier, String key) {
    return carrier.request().headers().get(key);
  }

  @Override
  public Iterator<String> getAll(@Nullable HttpRequestAndChannel carrier, String key) {
    List<String> list = carrier.request().headers().getAll(key);
    return list != null ? list.iterator() : Collections.emptyIterator();
  }
}
