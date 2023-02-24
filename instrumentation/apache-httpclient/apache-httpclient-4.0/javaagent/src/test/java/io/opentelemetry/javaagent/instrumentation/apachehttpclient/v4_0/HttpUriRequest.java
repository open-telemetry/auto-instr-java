/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.v4_0;

import java.net.URI;
import org.apache.http.client.methods.HttpRequestBase;

final class HttpUriRequest extends HttpRequestBase {

  private final String methodName;

  HttpUriRequest(String methodName, URI uri) {
    this.methodName = methodName;
    setURI(uri);
  }

  @Override
  public String getMethod() {
    return methodName;
  }
}
