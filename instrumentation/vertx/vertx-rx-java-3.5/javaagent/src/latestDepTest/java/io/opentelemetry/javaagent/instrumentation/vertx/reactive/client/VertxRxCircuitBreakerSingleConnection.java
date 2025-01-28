/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.vertx.reactive.client;

import io.vertx.core.AsyncResult;
import io.vertx.reactivex.circuitbreaker.CircuitBreaker;
import io.vertx.reactivex.ext.web.client.HttpRequest;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class VertxRxCircuitBreakerSingleConnection extends VertxRxSingleConnection {
  private final CircuitBreaker breaker;

  public VertxRxCircuitBreakerSingleConnection(String host, int port, CircuitBreaker breaker) {
    super(host, port);
    this.breaker = breaker;
  }

  @Override
  protected HttpResponse<?> fetchResponse(HttpRequest<?> request) {
    CompletableFuture<Object> future = new CompletableFuture<>();

    sendRequestWithCallback(
        request,
        it -> {
          if (it.succeeded()) {
            future.complete(it.result());
          } else {
            future.completeExceptionally(it.cause());
          }
        });

    return (HttpResponse<?>) future.join();
  }

  private void sendRequestWithCallback(HttpRequest<?> request, Consumer<AsyncResult<?>> consumer) {
    breaker.execute(
        command ->
            request.rxSend().doOnSuccess(command::complete).doOnError(command::fail).subscribe(),
        consumer::accept);
  }
}
