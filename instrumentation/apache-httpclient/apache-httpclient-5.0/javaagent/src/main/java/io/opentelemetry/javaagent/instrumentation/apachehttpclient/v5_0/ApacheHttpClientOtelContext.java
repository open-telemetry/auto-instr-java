/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.v5_0;

import io.opentelemetry.javaagent.instrumentation.apachehttpclient.commons.OtelHttpContext;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.http.protocol.HttpCoreContext;

public final class ApacheHttpClientOtelContext extends OtelHttpContext {
  private final HttpCoreContext httpContext;

  private ApacheHttpClientOtelContext(HttpContext httpContext) {
    this.httpContext = HttpCoreContext.adapt(httpContext);
  }

  public static ApacheHttpClientOtelContext adapt(HttpContext httpContext) {
    return new ApacheHttpClientOtelContext(httpContext);
  }

  @Override
  protected <T> void setAttribute(String name, T value) {
    httpContext.setAttribute(name, value);
  }

  @Override
  protected <T> T getAttribute(String name, Class<T> type) {
    return httpContext.getAttribute(name, type);
  }

  @Override
  protected void removeAttribute(String name) {
    httpContext.removeAttribute(name);
  }
}
