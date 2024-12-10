/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.ratpack.v1_7;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.ratpack.v1_7.internal.OpenTelemetryExecInitializer;
import io.opentelemetry.instrumentation.ratpack.v1_7.internal.OpenTelemetryExecInterceptor;
import io.opentelemetry.instrumentation.ratpack.v1_7.internal.OpenTelemetryServerHandler;
import ratpack.exec.ExecInitializer;
import ratpack.exec.ExecInterceptor;
import ratpack.handling.Handler;
import ratpack.handling.HandlerDecorator;
import ratpack.http.Request;
import ratpack.http.Response;
import ratpack.registry.RegistrySpec;

/**
 * Entrypoint for instrumenting Ratpack server.
 *
 * <p>To apply OpenTelemetry instrumentation to a server, configure the {@link RegistrySpec} using
 * {@link #configureRegistry(RegistrySpec)}.
 *
 * <pre>{@code
 * RatpackServerTelemetry telemetry = RatpackServerTelemetry.create(OpenTelemetrySdk.builder()
 *   ...
 *   .build());
 * RatpackServer.start(server -> {
 *   server.registryOf(telemetry::configureRegistry);
 *   server.handlers(chain -> ...);
 * });
 * }</pre>
 */
public final class RatpackServerTelemetry {

  /**
   * Returns a new {@link RatpackServerTelemetry} configured with the given {@link OpenTelemetry}.
   */
  public static RatpackServerTelemetry create(OpenTelemetry openTelemetry) {
    return builder(openTelemetry).build();
  }

  /**
   * Returns a new {@link RatpackServerTelemetryBuilder} configured with the given {@link
   * OpenTelemetry}.
   */
  public static RatpackServerTelemetryBuilder builder(OpenTelemetry openTelemetry) {
    return new RatpackServerTelemetryBuilder(openTelemetry);
  }

  private final OpenTelemetryServerHandler serverHandler;

  RatpackServerTelemetry(Instrumenter<Request, Response> serverInstrumenter) {
    serverHandler = new OpenTelemetryServerHandler(serverInstrumenter);
  }

  /** Returns a {@link Handler} to support Ratpack Registry binding. */
  public Handler getHandler() {
    return serverHandler;
  }

  /** Returns instance of {@link ExecInterceptor} to support Ratpack Registry binding. */
  public ExecInterceptor getExecInterceptor() {
    return OpenTelemetryExecInterceptor.INSTANCE;
  }

  /** Returns instance of {@link ExecInitializer} to support Ratpack Registry binding. */
  public ExecInitializer getExecInitializer() {
    return OpenTelemetryExecInitializer.INSTANCE;
  }

  /** Configures the {@link RegistrySpec} with OpenTelemetry. */
  public void configureRegistry(RegistrySpec registry) {
    registry.add(HandlerDecorator.prepend(serverHandler));
    registry.add(OpenTelemetryExecInterceptor.INSTANCE);
  }
}
