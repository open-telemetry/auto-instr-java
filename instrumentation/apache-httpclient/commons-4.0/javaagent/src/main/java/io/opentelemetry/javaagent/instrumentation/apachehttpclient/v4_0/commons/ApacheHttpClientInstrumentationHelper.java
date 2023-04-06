/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.v4_0.commons;

import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.javaagent.instrumentation.apachehttpclient.commons.OtelHttpRequest;
import io.opentelemetry.javaagent.instrumentation.apachehttpclient.commons.OtelHttpResponse;
import javax.annotation.Nullable;
import org.apache.http.HttpResponse;

public final class ApacheHttpClientInstrumentationHelper {
  private final Instrumenter<OtelHttpRequest, OtelHttpResponse> instrumenter;

  public ApacheHttpClientInstrumentationHelper(
      Instrumenter<OtelHttpRequest, OtelHttpResponse> instrumenter) {
    this.instrumenter = instrumenter;
  }

  @Nullable
  public Context startInstrumentation(Context parentContext, ApacheHttpClientRequest otelRequest) {
    if (!instrumenter.shouldStart(parentContext, otelRequest)) {
      return null;
    }

    return instrumenter.start(parentContext, otelRequest);
  }

  public <T> void endInstrumentation(
      Context context, ApacheHttpClientRequest otelRequest, T result, Throwable throwable) {
    OtelHttpResponse finalResponse = getFinalResponse(result);
    instrumenter.end(context, otelRequest, finalResponse, throwable);
  }

  private static <T> OtelHttpResponse getFinalResponse(T result) {
    if (result instanceof HttpResponse) {
      return new ApacheHttpClientResponse((HttpResponse) result);
    }
    return null;
  }
}
