/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.apachehttpclient.v5_2;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import org.apache.hc.client5.http.impl.ChainElement;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.core5.http.HttpResponse;

/** Entrypoint for instrumenting Apache HTTP Client. */
public final class ApacheHttpClientTelemetry {

  /**
   * Returns a new {@link ApacheHttpClientTelemetry} configured with the given {@link
   * OpenTelemetry}.
   */
  public static ApacheHttpClientTelemetry create(OpenTelemetry openTelemetry) {
    return builder(openTelemetry).build();
  }

  /**
   * Returns a new {@link ApacheHttpClientTelemetryBuilder} configured with the given {@link
   * OpenTelemetry}.
   */
  public static ApacheHttpClientTelemetryBuilder builder(OpenTelemetry openTelemetry) {
    return new ApacheHttpClientTelemetryBuilder(openTelemetry);
  }

  private final Instrumenter<ApacheHttpClientRequest, HttpResponse> instrumenter;
  private final ContextPropagators propagators;

  ApacheHttpClientTelemetry(
      Instrumenter<ApacheHttpClientRequest, HttpResponse> instrumenter,
      ContextPropagators propagators) {
    this.instrumenter = instrumenter;
    this.propagators = propagators;
  }

  /** Returns a new {@link CloseableHttpClient} with tracing configured. */
  public CloseableHttpClient newHttpClient() {
    return newHttpClientBuilder().build();
  }

  /** Returns a new {@link HttpClientBuilder} to create a client with tracing configured. */
  public HttpClientBuilder newHttpClientBuilder() {
    return HttpClientBuilder.create()
        .addExecInterceptorAfter(
            ChainElement.PROTOCOL.name(),
            "OtelExecChainHandler",
            new OtelExecChainHandler(instrumenter, propagators));
  }
}
