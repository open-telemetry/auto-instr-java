/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.commons;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientMetrics;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpSpanStatusExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.net.PeerServiceAttributesExtractor;
import io.opentelemetry.javaagent.bootstrap.internal.CommonConfig;

public final class ApacheHttpClientInstrumenter {
  public static Instrumenter<OtelHttpRequest, OtelHttpResponse> create(String instrumentationName) {
    ApacheHttpClientHttpAttributesGetter httpAttributesGetter;
    ApacheHttpClientNetAttributesGetter netAttributesGetter;

    httpAttributesGetter = new ApacheHttpClientHttpAttributesGetter();
    netAttributesGetter = new ApacheHttpClientNetAttributesGetter();

    return Instrumenter.<OtelHttpRequest, OtelHttpResponse>builder(
            GlobalOpenTelemetry.get(),
            instrumentationName,
            HttpSpanNameExtractor.create(httpAttributesGetter))
        .setSpanStatusExtractor(HttpSpanStatusExtractor.create(httpAttributesGetter))
        .addAttributesExtractor(
            HttpClientAttributesExtractor.builder(httpAttributesGetter, netAttributesGetter)
                .setCapturedRequestHeaders(CommonConfig.get().getClientRequestHeaders())
                .setCapturedResponseHeaders(CommonConfig.get().getClientResponseHeaders())
                .build())
        .addAttributesExtractor(
            PeerServiceAttributesExtractor.create(
                netAttributesGetter, CommonConfig.get().getPeerServiceMapping()))
        .addAttributesExtractor(new ApacheHttpClientContentLengthAttributesGetter())
        .addOperationMetrics(HttpClientMetrics.get())
        .buildClientInstrumenter(new HttpHeaderSetter());
  }

  private ApacheHttpClientInstrumenter() {}
}
