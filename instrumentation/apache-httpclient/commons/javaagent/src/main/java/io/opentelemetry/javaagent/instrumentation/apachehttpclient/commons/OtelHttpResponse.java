/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.commons;

import java.util.List;

public interface OtelHttpResponse {
  Integer statusCode();

  String getFlavour();

  List<String> getHeader(String name);
}
