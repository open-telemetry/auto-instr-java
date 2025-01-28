/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.vertx.reactive.server;

import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.EXCEPTION;

import io.opentelemetry.instrumentation.testing.junit.http.HttpServerTestOptions;
import io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint;
import io.vertx.circuitbreaker.CircuitBreakerOptions;
import io.vertx.core.Promise;
import io.vertx.reactivex.circuitbreaker.CircuitBreaker;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;

class VertxRxCircuitBreakerHttpServerTest extends VertxRxHttpServerTest {

  @Override
  protected void configure(HttpServerTestOptions options) {
    super.configure(options);

    options.setTestHttpPipelining(false);
    options.setHasExceptionOnServerSpan(endpoint -> endpoint != EXCEPTION);
  }

  @Override
  protected Class<? extends AbstractVerticle> verticle() {
    return VertxRxCircuitBreakerWebTestServer.class;
  }

  public static class VertxRxCircuitBreakerWebTestServer extends AbstractVertxRxVerticle {
    CircuitBreaker breaker;

    @Override
    void handle(RoutingContext ctx, ServerEndpoint endpoint, Runnable action) {
      breaker.execute(
          future -> future.complete(endpoint),
          result -> {
            if (result.failed()) {
              throw new IllegalStateException(result.cause());
            }
            controller(endpoint, action::run);
          });
    }

    @Override
    public void start(Promise<Void> startPromise) {
      int port = config().getInteger(CONFIG_HTTP_SERVER_PORT);
      Router router = Router.router(vertx);
      breaker =
          CircuitBreaker.create(
              "my-circuit-breaker",
              vertx,
              new CircuitBreakerOptions()
                  .setTimeout(
                      -1) // Disable the timeout otherwise it makes each test take this long.
              );

      configure(router);
      router
          .route(EXCEPTION.getPath())
          .handler(
              ctx ->
                  breaker.execute(
                      future -> future.fail(new IllegalStateException(EXCEPTION.getBody())),
                      result -> {
                        try {
                          Throwable cause = result.cause();
                          controller(
                              EXCEPTION,
                              () -> {
                                throw cause;
                              });
                        } catch (Throwable throwable) {
                          ctx.response()
                              .setStatusCode(EXCEPTION.getStatus())
                              .end(throwable.getMessage());
                        }
                      }));

      vertx
          .createHttpServer()
          .requestHandler(router)
          .listen(port, httpServerAsyncResult -> startPromise.complete());
    }
  }
}
