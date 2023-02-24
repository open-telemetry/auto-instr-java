/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpasyncclient;

import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.javaagent.instrumentation.apachehttpclient.commons.ApacheHttpClientInstrumenter;
import io.opentelemetry.javaagent.instrumentation.apachehttpclient.commons.OtelHttpRequest;
import io.opentelemetry.javaagent.instrumentation.apachehttpclient.commons.OtelHttpResponse;
import io.opentelemetry.javaagent.instrumentation.apachehttpclient.v4_0.commons.ApacheHttpClientInstrumentationHelper;

public final class ApacheHttpAsyncClientSingletons {
  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.apache-httpasyncclient-4.1";

  private static final ApacheHttpClientInstrumentationHelper HELPER;

  static {
    Instrumenter<OtelHttpRequest, OtelHttpResponse> intrumenter;
    intrumenter = ApacheHttpClientInstrumenter.create(INSTRUMENTATION_NAME);
    HELPER = new ApacheHttpClientInstrumentationHelper(intrumenter);
  }

  public static ApacheHttpClientInstrumentationHelper helper() {
    return HELPER;
  }

  private ApacheHttpAsyncClientSingletons() {}
}
