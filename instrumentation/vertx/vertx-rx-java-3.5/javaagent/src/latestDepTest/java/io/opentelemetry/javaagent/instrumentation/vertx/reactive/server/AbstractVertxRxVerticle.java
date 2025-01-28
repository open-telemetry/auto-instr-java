/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.vertx.reactive.server;

import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.CAPTURE_HEADERS;
import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.ERROR;
import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.INDEXED_CHILD;
import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.PATH_PARAM;
import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.QUERY_PARAM;
import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.REDIRECT;
import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.SUCCESS;

import io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint;
import io.vertx.reactivex.core.AbstractVerticle;
import io.vertx.reactivex.ext.web.Router;
import io.vertx.reactivex.ext.web.RoutingContext;

abstract class AbstractVertxRxVerticle extends AbstractVerticle {

  abstract void handle(RoutingContext ctx, ServerEndpoint endpoint, Runnable action);

  void configure(Router router) {
    router
        .route(SUCCESS.getPath())
        .handler(
            ctx ->
                handle(
                    ctx,
                    SUCCESS,
                    () ->
                        ctx.response().setStatusCode(SUCCESS.getStatus()).end(SUCCESS.getBody())));

    router
        .route(INDEXED_CHILD.getPath())
        .handler(
            ctx ->
                handle(
                    ctx,
                    INDEXED_CHILD,
                    () -> {
                      INDEXED_CHILD.collectSpanAttributes(
                          parameter -> ctx.request().params().get(parameter));
                      ctx.response().setStatusCode(INDEXED_CHILD.getStatus()).end();
                    }));

    router
        .route(QUERY_PARAM.getPath())
        .handler(
            ctx ->
                handle(
                    ctx,
                    QUERY_PARAM,
                    () ->
                        ctx.response()
                            .setStatusCode(QUERY_PARAM.getStatus())
                            .end(ctx.request().query())));

    router
        .route(REDIRECT.getPath())
        .handler(
            ctx ->
                handle(
                    ctx,
                    REDIRECT,
                    () ->
                        ctx.response()
                            .setStatusCode(REDIRECT.getStatus())
                            .putHeader("location", REDIRECT.getBody())
                            .end()));

    router
        .route(ERROR.getPath())
        .handler(
            ctx ->
                handle(
                    ctx,
                    ERROR,
                    () -> ctx.response().setStatusCode(ERROR.getStatus()).end(ERROR.getBody())));

    router
        .route("/path/:id/param")
        .handler(
            ctx ->
                handle(
                    ctx,
                    PATH_PARAM,
                    () ->
                        ctx.response()
                            .setStatusCode(PATH_PARAM.getStatus())
                            .end(ctx.request().getParam("id"))));

    router
        .route(CAPTURE_HEADERS.getPath())
        .handler(
            ctx ->
                handle(
                    ctx,
                    CAPTURE_HEADERS,
                    () ->
                        ctx.response()
                            .setStatusCode(CAPTURE_HEADERS.getStatus())
                            .putHeader("X-Test-Response", ctx.request().getHeader("X-Test-Request"))
                            .end(CAPTURE_HEADERS.getBody())));
  }
}
