/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.armeria.v1_3;

import com.linecorp.armeria.client.ClientRequestContext;
import com.linecorp.armeria.client.HttpClient;
import com.linecorp.armeria.common.logging.RequestLog;
import com.linecorp.armeria.server.HttpService;
import com.linecorp.armeria.server.ServiceRequestContext;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import java.util.function.Function;

/**
 * Entrypoint for instrumenting Armeria services or clients.
 *
 * @deprecated Use {@link ArmeriaClientTelemetry} and {@link ArmeriaServerTelemetry} instead.
 */
@Deprecated
public final class ArmeriaTelemetry {

  /**
   * Returns a new {@link ArmeriaTelemetry} configured with the given {@link OpenTelemetry}.
   *
   * @deprecated Use {@link ArmeriaClientTelemetry#create(OpenTelemetry)} and {@link
   *     ArmeriaServerTelemetry#create(OpenTelemetry)} instead.
   */
  @Deprecated
  public static ArmeriaTelemetry create(OpenTelemetry openTelemetry) {
    return builder(openTelemetry).build();
  }

  /**
   * @deprecated Use {@link ArmeriaClientTelemetry#builder(OpenTelemetry)} and {@link
   *     ArmeriaServerTelemetry#builder(OpenTelemetry)} instead.
   */
  @Deprecated
  public static ArmeriaTelemetryBuilder builder(OpenTelemetry openTelemetry) {
    return new ArmeriaTelemetryBuilder(openTelemetry);
  }

  private final Instrumenter<ClientRequestContext, RequestLog> clientInstrumenter;
  private final Instrumenter<ServiceRequestContext, RequestLog> serverInstrumenter;

  ArmeriaTelemetry(
      Instrumenter<ClientRequestContext, RequestLog> clientInstrumenter,
      Instrumenter<ServiceRequestContext, RequestLog> serverInstrumenter) {
    this.clientInstrumenter = clientInstrumenter;
    this.serverInstrumenter = serverInstrumenter;
  }

  /**
   * Returns a new {@link HttpClient} decorator for use with methods like {@link
   * com.linecorp.armeria.client.ClientBuilder#decorator(Function)}.
   *
   * @deprecated Use {@link ArmeriaClientTelemetry#newDecorator()} instead.
   */
  @Deprecated
  public Function<? super HttpClient, ? extends HttpClient> newClientDecorator() {
    return client -> new OpenTelemetryClient(client, clientInstrumenter);
  }

  /**
   * Returns a new {@link HttpService} decorator for use with methods like {@link
   * HttpService#decorate(Function)}.
   *
   * @deprecated Use {@link ArmeriaServerTelemetry#newDecorator()} instead.
   */
  @Deprecated
  public Function<? super HttpService, ? extends HttpService> newServiceDecorator() {
    return service -> new OpenTelemetryService(service, serverInstrumenter);
  }
}
