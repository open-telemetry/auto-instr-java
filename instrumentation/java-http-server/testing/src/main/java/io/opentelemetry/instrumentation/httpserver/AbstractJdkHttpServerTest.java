/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.httpserver;

import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.CAPTURE_HEADERS;
import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.ERROR;
import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.INDEXED_CHILD;
import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.PATH_PARAM;
import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.QUERY_PARAM;
import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.REDIRECT;
import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.SUCCESS;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.instrumentation.testing.junit.http.AbstractHttpServerTest;
import io.opentelemetry.instrumentation.testing.junit.http.HttpServerTestOptions;
import io.opentelemetry.testing.internal.armeria.common.QueryParams;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

public abstract class AbstractJdkHttpServerTest extends AbstractHttpServerTest<HttpServer> {

  protected Filter customFilter() {
    return null;
  }

  static void sendResponse(HttpExchange exchange, int status, String response) throws IOException {
    sendResponse(exchange, status, Collections.emptyMap(), response);
  }

  static void sendResponse(HttpExchange exchange, int status, Map<String, String> headers)
      throws IOException {
    sendResponse(exchange, status, headers, "");
  }

  static void sendResponse(
      HttpExchange exchange, int status, Map<String, String> headers, String response)
      throws IOException {

    byte[] bytes = response.getBytes(StandardCharsets.UTF_8);

    // -1 means no content, 0 means unknown content length
    long contentLength = bytes.length == 0 ? -1 : bytes.length;
    exchange.getResponseHeaders().set("Content-Type", "text/plain");
    headers.forEach(exchange.getResponseHeaders()::set);
    try (OutputStream os = exchange.getResponseBody()) {
      exchange.sendResponseHeaders(status, contentLength);
      os.write(bytes);
    }
  }

  private static String getUrlQuery(HttpExchange exchange) {
    return exchange.getRequestURI().getQuery();
  }

  @Override
  protected HttpServer setupServer() throws IOException {

    List<HttpContext> contexts = new ArrayList<>();
    HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

    server.setExecutor(Executors.newCachedThreadPool());
    HttpContext context =
        server.createContext(
            SUCCESS.getPath(),
            ctx ->
                testing()
                    .runWithSpan(
                        "controller",
                        () -> sendResponse(ctx, SUCCESS.getStatus(), SUCCESS.getBody())));

    contexts.add(context);
    context =
        server.createContext(
            REDIRECT.getPath(),
            ctx ->
                testing()
                    .runWithSpan(
                        "controller",
                        () ->
                            sendResponse(
                                ctx,
                                REDIRECT.getStatus(),
                                Collections.singletonMap("Location", REDIRECT.getBody()))));

    contexts.add(context);
    context =
        server.createContext(
            ERROR.getPath(),
            ctx ->
                testing()
                    .runWithSpan(
                        "controller", () -> sendResponse(ctx, ERROR.getStatus(), ERROR.getBody())));

    contexts.add(context);
    context =
        server.createContext(
            "/query",
            ctx ->
                testing()
                    .runWithSpan(
                        "controller",
                        () ->
                            sendResponse(
                                ctx,
                                QUERY_PARAM.getStatus(),
                                "some="
                                    + QueryParams.fromQueryString(getUrlQuery(ctx)).get("some"))));
    contexts.add(context);
    context =
        server.createContext(
            "/path/:id/param",
            ctx ->
                testing()
                    .runWithSpan(
                        "controller", () -> sendResponse(ctx, PATH_PARAM.getStatus(), "id")));
    contexts.add(context);
    context =
        server.createContext(
            "/child",
            ctx ->
                testing()
                    .runWithSpan(
                        "controller",
                        () -> {
                          INDEXED_CHILD.collectSpanAttributes(
                              name -> QueryParams.fromQueryString(getUrlQuery(ctx)).get(name));

                          sendResponse(ctx, INDEXED_CHILD.getStatus(), INDEXED_CHILD.getBody());
                        }));
    contexts.add(context);
    context =
        server.createContext(
            "/captureHeaders",
            ctx ->
                testing()
                    .runWithSpan(
                        "controller",
                        () ->
                            sendResponse(
                                ctx,
                                CAPTURE_HEADERS.getStatus(),
                                Collections.singletonMap(
                                    "X-Test-Response",
                                    ctx.getRequestHeaders().getFirst("X-Test-Request")),
                                CAPTURE_HEADERS.getBody())));

    contexts.add(context);

    Filter customFilter = customFilter();
    if (customFilter != null) {
      contexts.forEach(ctx -> ctx.getFilters().add(customFilter));
    }

    // Make sure user decorators see spans.
    Filter spanFilter = new SpanFilter();

    contexts.forEach(ctx -> ctx.getFilters().add(spanFilter));
    server.start();

    return server;
  }

  @Override
  protected void stopServer(HttpServer server) {
    CompletableFuture.runAsync(() -> server.stop(1000));
  }

  @Override
  protected void configure(HttpServerTestOptions options) {

    options.setTestNotFound(false);
    options.setTestPathParam(false);
    options.setTestException(false);
  }

  static class SpanFilter extends Filter {

    @Override
    public void doFilter(HttpExchange exchange, Chain chain) throws IOException {

      if (!Span.current().getSpanContext().isValid()) {
        // Return an invalid code to fail any assertion

        exchange.sendResponseHeaders(601, -1);
      }

      try {
        chain.doFilter(exchange);
      } catch (Exception e) {
        sendResponse(exchange, 500, e.getMessage());
      }

      if (exchange.getResponseCode() == -1) {

        sendResponse(exchange, 500, "nothing");
      }
    }

    @Override
    public String description() {
      return "test";
    }
  }
}
