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

/**
 * Entrypoint for instrumenting Apache HTTP Client.
 *
 * @deprecated Use {@link ApacheHttpClientTelemetry} instead.
 */
@Deprecated
public final class ApacheHttpClient5Telemetry {

  /**
   * Returns a new {@link ApacheHttpClient5Telemetry} configured with the given {@link
   * OpenTelemetry}.
   */
  public static ApacheHttpClient5Telemetry create(OpenTelemetry openTelemetry) {
    return builder(openTelemetry).build();
  }

  /**
   * Returns a new {@link ApacheHttpClient5TelemetryBuilder} configured with the given {@link
   * OpenTelemetry}.
   */
  public static ApacheHttpClient5TelemetryBuilder builder(OpenTelemetry openTelemetry) {
    return new ApacheHttpClient5TelemetryBuilder(openTelemetry);
  }

  private final Instrumenter<ApacheHttpClient5Request, HttpResponse> instrumenter;
  private final ContextPropagators propagators;

  ApacheHttpClient5Telemetry(
      Instrumenter<ApacheHttpClient5Request, HttpResponse> instrumenter,
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
            new OtelExecChainHandlerOld(instrumenter, propagators));
  }
}
