/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.commons;

import java.util.List;

public interface OtelHttpRequest {
  BytesTransferMetrics getBytesTransferMetrics();

  String getPeerName();

  Integer getPeerPort();

  String getMethod();

  String getUrl();

  String getFlavor();

  List<String> getHeader(String name);

  String getFirstHeader(String name);

  void setHeader(String key, String value);
}
