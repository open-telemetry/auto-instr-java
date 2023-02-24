/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.v5_0;

import io.opentelemetry.javaagent.instrumentation.apachehttpclient.commons.OtelHttpContextManager;
import org.apache.hc.core5.http.protocol.HttpContext;

public final class ApacheHttpClientContextManager extends OtelHttpContextManager<HttpContext> {
  private static final ApacheHttpClientContextManager INSTANCE;

  static {
    INSTANCE = new ApacheHttpClientContextManager();
  }

  private ApacheHttpClientContextManager() {
    super(ApacheHttpClientOtelContext::adapt);
  }

  public static OtelHttpContextManager<HttpContext> httpContextManager() {
    return INSTANCE;
  }
}
