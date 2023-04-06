/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.commons;

import java.net.InetSocketAddress;
import java.util.List;

public interface OtelHttpRequest {
  String getPeerName();

  Integer getPeerPort();

  InetSocketAddress getPeerSocketAddress();

  String getMethod();

  String getUrl();

  String getFlavor();

  List<String> getHeader(String name);

  void setHeader(String key, String value);
}
