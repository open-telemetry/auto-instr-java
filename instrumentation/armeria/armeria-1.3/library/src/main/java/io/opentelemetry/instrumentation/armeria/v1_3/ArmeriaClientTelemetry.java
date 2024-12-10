/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.armeria.v1_3;

import com.linecorp.armeria.client.ClientRequestContext;
import com.linecorp.armeria.client.HttpClient;
import com.linecorp.armeria.common.logging.RequestLog;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import java.util.function.Function;

/** Entrypoint for instrumenting Armeria clients. */
public final class ArmeriaClientTelemetry {

  /**
   * Returns a new {@link ArmeriaClientTelemetry} configured with the given {@link OpenTelemetry}.
   */
  public static ArmeriaClientTelemetry create(OpenTelemetry openTelemetry) {
    return builder(openTelemetry).build();
  }

  public static ArmeriaClientTelemetryBuilder builder(OpenTelemetry openTelemetry) {
    return new ArmeriaClientTelemetryBuilder(openTelemetry);
  }

  private final Instrumenter<ClientRequestContext, RequestLog> instrumenter;

  ArmeriaClientTelemetry(Instrumenter<ClientRequestContext, RequestLog> instrumenter) {
    this.instrumenter = instrumenter;
  }

  /**
   * Returns a new {@link HttpClient} decorator for use with methods like {@link
   * com.linecorp.armeria.client.ClientBuilder#decorator(Function)}.
   */
  public Function<? super HttpClient, ? extends HttpClient> newDecorator() {
    return client -> new OpenTelemetryClient(client, instrumenter);
  }
}
