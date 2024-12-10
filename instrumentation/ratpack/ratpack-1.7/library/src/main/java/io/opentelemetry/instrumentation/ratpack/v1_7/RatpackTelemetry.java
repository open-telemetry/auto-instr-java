/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.ratpack.v1_7;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.ratpack.v1_7.internal.OpenTelemetryExecInitializer;
import io.opentelemetry.instrumentation.ratpack.v1_7.internal.OpenTelemetryExecInterceptor;
import io.opentelemetry.instrumentation.ratpack.v1_7.internal.OpenTelemetryHttpClient;
import io.opentelemetry.instrumentation.ratpack.v1_7.internal.OpenTelemetryServerHandler;
import ratpack.exec.ExecInitializer;
import ratpack.exec.ExecInterceptor;
import ratpack.handling.HandlerDecorator;
import ratpack.http.Request;
import ratpack.http.Response;
import ratpack.http.client.HttpClient;
import ratpack.http.client.HttpResponse;
import ratpack.http.client.RequestSpec;
import ratpack.registry.RegistrySpec;

/**
 * Entrypoint for instrumenting Ratpack server and http client.
 *
 * <p>To apply OpenTelemetry instrumentation to a server, configure the {@link RegistrySpec} using
 * {@link #configureServerRegistry(RegistrySpec)}.
 *
 * <pre>{@code
 * RatpackTelemetry telemetry = RatpackTelemetry.create(OpenTelemetrySdk.builder()
 *   ...
 *   .build());
 * RatpackServer.start(server -> {
 *   server.registryOf(telemetry::configureServerRegistry);
 *   server.handlers(chain -> ...);
 * });
 * }</pre>
 *
 * <p>To apply OpenTelemetry instrumentation to a http client, wrap the {@link HttpClient} using
 * {@link #instrumentHttpClient(HttpClient)}.
 *
 * <pre>{@code
 * RatpackTelemetry telemetry = RatpackTelemetry.create(OpenTelemetrySdk.builder()
 *   ...
 *   .build());
 * HttpClient instrumentedHttpClient = telemetry.instrumentHttpClient(httpClient);
 * }</pre>
 *
 * @deprecated Use {@link RatpackClientTelemetry} and {@link RatpackServerTelemetry} instead.
 */
@Deprecated
public final class RatpackTelemetry {

  /**
   * Returns a new {@link RatpackTelemetry} configured with the given {@link OpenTelemetry}.
   *
   * @deprecated Use {@link RatpackClientTelemetry#create(OpenTelemetry)} and {@link
   *     RatpackServerTelemetry#create(OpenTelemetry)} instead.
   */
  @Deprecated
  public static RatpackTelemetry create(OpenTelemetry openTelemetry) {
    return builder(openTelemetry).build();
  }

  /**
   * Returns a new {@link RatpackTelemetryBuilder} configured with the given {@link OpenTelemetry}.
   *
   * @deprecated Use {@link RatpackClientTelemetry#builder(OpenTelemetry)} and {@link
   *     RatpackServerTelemetry#builder(OpenTelemetry)} instead.
   */
  @Deprecated
  public static RatpackTelemetryBuilder builder(OpenTelemetry openTelemetry) {
    return new RatpackTelemetryBuilder(openTelemetry);
  }

  private final OpenTelemetryServerHandler serverHandler;
  private final OpenTelemetryHttpClient httpClientInstrumenter;

  RatpackTelemetry(
      Instrumenter<Request, Response> serverInstrumenter,
      Instrumenter<RequestSpec, HttpResponse> clientInstrumenter) {
    serverHandler = new OpenTelemetryServerHandler(serverInstrumenter);
    httpClientInstrumenter = new OpenTelemetryHttpClient(clientInstrumenter);
  }

  /**
   * Returns instance of {@link OpenTelemetryServerHandler} to support Ratpack Registry binding.
   *
   * @deprecated Use {@link RatpackServerTelemetry#getHandler()} instead.
   */
  @Deprecated
  public OpenTelemetryServerHandler getOpenTelemetryServerHandler() {
    return serverHandler;
  }

  /**
   * Returns instance of {@link ExecInterceptor} to support Ratpack Registry binding.
   *
   * @deprecated Use {@link RatpackServerTelemetry#getExecInterceptor()} instead.
   */
  @Deprecated
  public ExecInterceptor getOpenTelemetryExecInterceptor() {
    return OpenTelemetryExecInterceptor.INSTANCE;
  }

  /**
   * Returns instance of {@link ExecInitializer} to support Ratpack Registry binding.
   *
   * @deprecated Use {@link RatpackServerTelemetry#getExecInitializer()} instead.
   */
  @Deprecated
  public ExecInitializer getOpenTelemetryExecInitializer() {
    return OpenTelemetryExecInitializer.INSTANCE;
  }

  /**
   * Configures the {@link RegistrySpec} with OpenTelemetry.
   *
   * @deprecated Use {@link RatpackServerTelemetry#configureRegistry(RegistrySpec)} instead.
   */
  @Deprecated
  public void configureServerRegistry(RegistrySpec registry) {
    registry.add(HandlerDecorator.prepend(serverHandler));
    registry.add(OpenTelemetryExecInterceptor.INSTANCE);
    registry.add(OpenTelemetryExecInitializer.INSTANCE);
  }

  /**
   * Returns instrumented instance of {@link HttpClient} with OpenTelemetry.
   *
   * @deprecated Use {@link RatpackClientTelemetry#instrument(HttpClient)} instead.
   */
  @Deprecated
  public HttpClient instrumentHttpClient(HttpClient httpClient) throws Exception {
    return httpClientInstrumenter.instrument(httpClient);
  }
}
