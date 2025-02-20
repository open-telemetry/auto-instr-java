/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.play.v2_4.server;

import static io.opentelemetry.api.trace.SpanKind.INTERNAL;
import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.CAPTURE_HEADERS;
import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.ERROR;
import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.EXCEPTION;
import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.INDEXED_CHILD;
import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.QUERY_PARAM;
import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.REDIRECT;
import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.SUCCESS;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_ROUTE;
import static io.opentelemetry.semconv.incubating.CodeIncubatingAttributes.CODE_FUNCTION;
import static io.opentelemetry.semconv.incubating.CodeIncubatingAttributes.CODE_NAMESPACE;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.http.AbstractHttpServerTest;
import io.opentelemetry.instrumentation.testing.junit.http.HttpServerInstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.http.HttpServerTestOptions;
import io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint;
import io.opentelemetry.sdk.testing.assertj.SpanDataAssert;
import io.opentelemetry.sdk.trace.data.StatusData;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.extension.RegisterExtension;
import play.mvc.Results;
import play.routing.RoutingDsl;
import play.server.Server;

class PlayServerTest extends AbstractHttpServerTest<Server> {

  @RegisterExtension
  static final InstrumentationExtension testing = HttpServerInstrumentationExtension.forAgent();

  @Override
  protected Server setupServer() {
    RoutingDsl router =
        new RoutingDsl()
            .GET(SUCCESS.getPath())
            .routeTo(
                () ->
                    controller(
                        SUCCESS, () -> Results.status(SUCCESS.getStatus(), SUCCESS.getBody())))
            .GET(INDEXED_CHILD.getPath())
            .routeTo(
                () ->
                    controller(
                        INDEXED_CHILD,
                        () -> {
                          INDEXED_CHILD.collectSpanAttributes(
                              it -> play.mvc.Http.Context.Implicit.request().getQueryString(it));
                          return Results.status(INDEXED_CHILD.getStatus());
                        }))
            .GET(QUERY_PARAM.getPath())
            .routeTo(
                () ->
                    controller(
                        QUERY_PARAM,
                        () -> Results.status(QUERY_PARAM.getStatus(), QUERY_PARAM.getBody())))
            .GET(REDIRECT.getPath())
            .routeTo(() -> controller(REDIRECT, () -> Results.found(REDIRECT.getBody())))
            .GET(CAPTURE_HEADERS.getPath())
            .routeTo(
                () ->
                    controller(
                        CAPTURE_HEADERS,
                        () ->
                            Results.status(CAPTURE_HEADERS.getStatus(), CAPTURE_HEADERS.getBody())
                                .withHeader(
                                    "X-Test-Response",
                                    play.mvc.Http.Context.Implicit.request()
                                        .getHeader("X-Test-Request"))))
            .GET(ERROR.getPath())
            .routeTo(
                () -> controller(ERROR, () -> Results.status(ERROR.getStatus(), ERROR.getBody())))
            .GET(EXCEPTION.getPath())
            .routeTo(
                () ->
                    controller(
                        EXCEPTION,
                        () -> {
                          throw new IllegalArgumentException(EXCEPTION.getBody());
                        }));

    return Server.forRouter(router.build(), port);
  }

  @Override
  protected void stopServer(Server server) {
    server.stop();
  }

  @Override
  protected void configure(HttpServerTestOptions options) {
    options.setHasHandlerSpan(unused -> true);
    options.setTestHttpPipelining(false);
    options.setResponseCodeOnNonStandardHttpMethod(404);
    options.setVerifyServerSpanEndTime(
        false); // server spans are ended inside of the controller spans
    options.setHttpAttributes(
        serverEndpoint -> {
          Set<AttributeKey<?>> attributes =
              new HashSet<>(HttpServerTestOptions.DEFAULT_HTTP_ATTRIBUTES);
          attributes.remove(HTTP_ROUTE);
          return attributes;
        });

    options.setExpectedException(new IllegalArgumentException(EXCEPTION.getBody()));
  }

  @Override
  @SuppressWarnings("deprecation") // uses deprecated semconv
  public SpanDataAssert assertHandlerSpan(
      SpanDataAssert span, String method, ServerEndpoint endpoint) {
    span.hasName("play.request")
        .hasKind(INTERNAL)
        .hasAttributesSatisfyingExactly(
            equalTo(CODE_NAMESPACE, "play.api.mvc.ActionBuilder$$anon$2"),
            equalTo(CODE_FUNCTION, "apply"));

    if (endpoint == EXCEPTION) {
      span.hasStatus(StatusData.error());
      span.hasException(new IllegalArgumentException(EXCEPTION.getBody()));
    }
    return span;
  }
}
