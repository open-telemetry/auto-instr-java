/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.vertx.reactive.client;

import io.opentelemetry.instrumentation.testing.junit.http.SingleConnection;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.HttpRequest;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import java.util.Map;
import java.util.Objects;

class VertxRxSingleConnection implements SingleConnection {
  private final WebClient webClient;
  private final String host;
  private final int port;

  public VertxRxSingleConnection(String host, int port) {
    this.host = host;
    this.port = port;

    WebClientOptions clientOptions =
        new WebClientOptions()
            .setConnectTimeout(5000)
            .setMaxPoolSize(1)
            .setKeepAlive(true)
            .setPipelining(true);

    Vertx vertx = Vertx.vertx(new VertxOptions());
    this.webClient = WebClient.create(vertx, clientOptions);
  }

  @Override
  public int doRequest(String path, Map<String, String> headers) {
    String requestId = Objects.requireNonNull(headers.get(REQUEST_ID_HEADER));

    HttpRequest<Buffer> request = webClient.request(HttpMethod.GET, port, host, path);
    headers.forEach(request::putHeader);

    HttpResponse<?> response = fetchResponse(request);

    String responseId = response.getHeader(REQUEST_ID_HEADER);
    if (!requestId.equals(responseId)) {
      throw new IllegalStateException(
          String.format("Received response with id %s, expected %s", responseId, requestId));
    }

    return response.statusCode();
  }

  protected HttpResponse<?> fetchResponse(HttpRequest<?> request) {
    return request.rxSend().blockingGet();
  }
}
