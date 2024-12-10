/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.armeria.v1_3;

import com.linecorp.armeria.common.logging.RequestLog;
import com.linecorp.armeria.server.HttpService;
import com.linecorp.armeria.server.ServiceRequestContext;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import java.util.function.Function;

/** Entrypoint for instrumenting Armeria services. */
public final class ArmeriaServerTelemetry {

  /**
   * Returns a new {@link ArmeriaServerTelemetry} configured with the given {@link OpenTelemetry}.
   */
  public static ArmeriaServerTelemetry create(OpenTelemetry openTelemetry) {
    return builder(openTelemetry).build();
  }

  public static ArmeriaServerTelemetryBuilder builder(OpenTelemetry openTelemetry) {
    return new ArmeriaServerTelemetryBuilder(openTelemetry);
  }

  private final Instrumenter<ServiceRequestContext, RequestLog> instrumenter;

  ArmeriaServerTelemetry(Instrumenter<ServiceRequestContext, RequestLog> instrumenter) {
    this.instrumenter = instrumenter;
  }

  /**
   * Returns a new {@link HttpService} decorator for use with methods like {@link
   * HttpService#decorate(Function)}.
   */
  public Function<? super HttpService, ? extends HttpService> newDecorator() {
    return service -> new OpenTelemetryService(service, instrumenter);
  }
}
