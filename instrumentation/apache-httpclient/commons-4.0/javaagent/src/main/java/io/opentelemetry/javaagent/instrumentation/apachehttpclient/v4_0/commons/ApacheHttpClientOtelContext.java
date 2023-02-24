/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.v4_0.commons;

import io.opentelemetry.javaagent.instrumentation.apachehttpclient.commons.OtelHttpContext;
import org.apache.http.protocol.HttpContext;

public final class ApacheHttpClientOtelContext extends OtelHttpContext {
  private final HttpContext httpContext;

  private ApacheHttpClientOtelContext(HttpContext httpContext) {
    this.httpContext = httpContext;
  }

  public static ApacheHttpClientOtelContext adapt(HttpContext httpContext) {
    return new ApacheHttpClientOtelContext(httpContext);
  }

  @Override
  protected <T> void setAttribute(String name, T value) {
    httpContext.setAttribute(name, value);
  }

  @Override
  protected <T> T getAttribute(String attributeName, Class<T> clazz) {
    Object attribute = httpContext.getAttribute(attributeName);
    if (attribute == null) {
      return null;
    }
    return clazz.cast(attribute);
  }

  @Override
  protected void removeAttribute(String name) {
    httpContext.removeAttribute(name);
  }
}
