/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.spring.webflux.v5_3.internal;

import io.opentelemetry.instrumentation.api.incubator.builder.internal.DefaultHttpClientInstrumenterBuilder;
import io.opentelemetry.instrumentation.api.incubator.builder.internal.DefaultHttpServerInstrumenterBuilder;
import io.opentelemetry.instrumentation.spring.webflux.v5_3.SpringWebfluxTelemetryBuilder;
import java.util.function.Function;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.server.ServerWebExchange;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public class SpringWebfluxUtil {
  private SpringWebfluxUtil() {}

  private static Function<
          SpringWebfluxTelemetryBuilder,
          DefaultHttpClientInstrumenterBuilder<ClientRequest, ClientResponse>>
      clientBuilderExtractor;

  private static Function<
          SpringWebfluxTelemetryBuilder,
          DefaultHttpServerInstrumenterBuilder<ServerWebExchange, ServerWebExchange>>
      serverBuilderExtractor;

  public static Function<
          SpringWebfluxTelemetryBuilder,
          DefaultHttpServerInstrumenterBuilder<ServerWebExchange, ServerWebExchange>>
      getServerBuilderExtractor() {
    return serverBuilderExtractor;
  }

  public static void setServerBuilderExtractor(
      Function<
              SpringWebfluxTelemetryBuilder,
              DefaultHttpServerInstrumenterBuilder<ServerWebExchange, ServerWebExchange>>
          serverBuilderExtractor) {
    SpringWebfluxUtil.serverBuilderExtractor = serverBuilderExtractor;
  }

  public static Function<
          SpringWebfluxTelemetryBuilder,
          DefaultHttpClientInstrumenterBuilder<ClientRequest, ClientResponse>>
      getClientBuilderExtractor() {
    return clientBuilderExtractor;
  }

  public static void setClientBuilderExtractor(
      Function<
              SpringWebfluxTelemetryBuilder,
              DefaultHttpClientInstrumenterBuilder<ClientRequest, ClientResponse>>
          clientBuilderExtractor) {
    SpringWebfluxUtil.clientBuilderExtractor = clientBuilderExtractor;
  }
}
