/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.vertx.reactive.server;

import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.EXCEPTION;

import io.opentelemetry.instrumentation.api.internal.HttpConstants;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.http.AbstractHttpServerTest;
import io.opentelemetry.instrumentation.testing.junit.http.HttpServerInstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.http.HttpServerTestOptions;
import io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.extension.RegisterExtension;

class VertxRxHttpServerTest extends AbstractHttpServerTest<Vertx> {
  static final String CONFIG_HTTP_SERVER_PORT = "http.server.port";

  @RegisterExtension
  static final InstrumentationExtension testing = HttpServerInstrumentationExtension.forAgent();

  @Override
  protected Vertx setupServer() throws Exception {
    Vertx server =
        Vertx.vertx(
            new VertxOptions()
                // Useful for debugging:
                // .setBlockedThreadCheckInterval(Integer.MAX_VALUE)
                .setClusterPort(port));
    CompletableFuture<Void> future = new CompletableFuture<>();
    server.deployVerticle(
        verticle().getName(),
        new DeploymentOptions()
            .setConfig(new JsonObject().put(CONFIG_HTTP_SERVER_PORT, port))
            .setInstances(3),
        result -> {
          if (!result.succeeded()) {
            throw new IllegalStateException("Cannot deploy server Verticle", result.cause());
          }
          future.complete(null);
        });

    future.get(30, TimeUnit.SECONDS);
    return server;
  }

  @Override
  protected void stopServer(Vertx vertx) {
    vertx.close();
  }

  @Override
  protected void configure(HttpServerTestOptions options) {
    super.configure(options);

    options.setTestPathParam(true);
    // server spans are ended inside the controller spans
    options.setVerifyServerSpanEndTime(false);
    options.setExpectedHttpRoute(
        (endpoint, method) -> {
          if (HttpConstants._OTHER.equals(method)) {
            return getContextPath() + endpoint.getPath();
          }
          return expectedHttpRoute(endpoint, method);
        });
  }

  protected Class<? extends AbstractVerticle> verticle() {
    return VertxReactiveWebServer.class;
  }

  public static class VertxReactiveWebServer extends AbstractVertxRxVerticle {
    @Override
    void handle(RoutingContext ctx, ServerEndpoint endpoint, Runnable action) {
      controller(endpoint, action::run);
    }

    @Override
    public void start(Future<Void> startFuture) {
      int port = config().getInteger(CONFIG_HTTP_SERVER_PORT);
      Router router = Router.router(vertx);

      configure(router);
      router
          .route(EXCEPTION.getPath())
          .handler(
              ctx ->
                  handle(
                      ctx,
                      EXCEPTION,
                      () -> {
                        throw new IllegalStateException(EXCEPTION.getBody());
                      }));

      vertx
          .createHttpServer()
          .requestHandler(router::accept)
          .listen(port, httpServerAsyncResult -> startFuture.complete());
    }
  }
}
