/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.activejhttp;

import io.activej.http.HttpHeader;
import io.activej.http.HttpHeaderValue;
import io.activej.http.HttpHeaders;
import io.activej.http.HttpRequest;
import io.opentelemetry.context.propagation.internal.ExtendedTextMapGetter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

enum ActivejHttpServerHeaders implements ExtendedTextMapGetter<HttpRequest> {
  INSTANCE;

  @Override
  public Iterable<String> keys(HttpRequest httpRequest) {
    return httpRequest.getHeaders().stream().map(h -> h.getKey().toString()).toList();
  }

  @Override
  public String get(HttpRequest carrier, String key) {
    if (carrier == null) {
      return null;
    }
    return carrier.getHeader(HttpHeaders.of(key));
  }

  @Override
  public Iterator<String> getAll(HttpRequest carrier, String key) {
    List<String> values = new ArrayList<>();
    if (carrier != null) {
      for (Map.Entry<HttpHeader, HttpHeaderValue> entry : carrier.getHeaders()) {
        if (entry.getKey().toString().equalsIgnoreCase(key)) {
          values.add(entry.getValue().toString());
        }
      }
    }
    return values.iterator();
  }
}
