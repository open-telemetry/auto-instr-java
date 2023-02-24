/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.v5_0;

import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.javaagent.instrumentation.apachehttpclient.commons.ApacheHttpClientInstrumenter;
import io.opentelemetry.javaagent.instrumentation.apachehttpclient.commons.OtelHttpRequest;
import io.opentelemetry.javaagent.instrumentation.apachehttpclient.commons.OtelHttpResponse;

public final class ApacheHttpClientSingletons {
  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.apache-httpclient-5.0";

  private static final ApacheHttpClientInstrumentationHelper HELPER;

  static {
    Instrumenter<OtelHttpRequest, OtelHttpResponse> instrumenter;
    instrumenter = ApacheHttpClientInstrumenter.create(INSTRUMENTATION_NAME);
    HELPER = new ApacheHttpClientInstrumentationHelper(instrumenter);
  }

  public static ApacheHttpClientInstrumentationHelper helper() {
    return HELPER;
  }

  private ApacheHttpClientSingletons() {}
}
