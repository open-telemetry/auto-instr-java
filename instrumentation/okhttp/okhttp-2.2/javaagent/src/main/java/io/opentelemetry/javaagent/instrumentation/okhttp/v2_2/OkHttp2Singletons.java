/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.okhttp.v2_2;

import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientExperimentalMetrics;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientPeerServiceAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientSemanticConvention;
import io.opentelemetry.javaagent.bootstrap.internal.CommonConfig;

public final class OkHttp2Singletons {

  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.okhttp-2.2";

  private static final TracingInterceptor TRACING_INTERCEPTOR;

  static {
    OkHttp2HttpAttributesGetter getter = new OkHttp2HttpAttributesGetter();
    OpenTelemetry openTelemetry = GlobalOpenTelemetry.get();

    Instrumenter<Request, Response> instrumenter =
        HttpClientSemanticConvention.create(openTelemetry, INSTRUMENTATION_NAME, getter)
            .setCapturedRequestHeaders(CommonConfig.get().getClientRequestHeaders())
            .setCapturedResponseHeaders(CommonConfig.get().getClientResponseHeaders())
            .setKnownMethods(CommonConfig.get().getKnownHttpRequestMethods())
            .configureInstrumenter(
                builder -> {
                  builder.addAttributesExtractor(
                      HttpClientPeerServiceAttributesExtractor.create(
                          getter, CommonConfig.get().getPeerServiceResolver()));
                  if (CommonConfig.get().shouldEmitExperimentalHttpClientMetrics()) {
                    builder.addOperationMetrics(HttpClientExperimentalMetrics.get());
                  }
                })
            .buildInstrumenter();

    TRACING_INTERCEPTOR = new TracingInterceptor(instrumenter, openTelemetry.getPropagators());
  }

  public static Interceptor tracingInterceptor() {
    return TRACING_INTERCEPTOR;
  }

  private OkHttp2Singletons() {}
}
