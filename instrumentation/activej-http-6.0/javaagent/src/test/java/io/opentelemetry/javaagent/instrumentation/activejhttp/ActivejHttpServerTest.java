/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.activejhttp;

import static io.activej.http.HttpMethod.GET;
import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.CAPTURE_HEADERS;
import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.ERROR;
import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.EXCEPTION;
import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.ID_PARAMETER_NAME;
import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.INDEXED_CHILD;
import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.NOT_FOUND;
import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.QUERY_PARAM;
import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.REDIRECT;
import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.SUCCESS;
import static io.opentelemetry.semconv.NetworkAttributes.NETWORK_PEER_PORT;

import io.activej.eventloop.Eventloop;
import io.activej.http.AsyncServlet;
import io.activej.http.HttpHeaderValue;
import io.activej.http.HttpHeaders;
import io.activej.http.HttpResponse;
import io.activej.http.HttpServer;
import io.activej.http.RoutingServlet;
import io.activej.promise.Promise;
import io.activej.reactor.Reactor;
import io.opentelemetry.instrumentation.api.internal.HttpConstants;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.http.AbstractHttpServerTest;
import io.opentelemetry.instrumentation.testing.junit.http.HttpServerInstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.http.HttpServerTestOptions;
import io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint;
import io.opentelemetry.testing.internal.armeria.internal.shaded.guava.collect.ImmutableSet;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ActivejHttpServerTest extends AbstractHttpServerTest<HttpServer> {

  @RegisterExtension static final EventloopExtension eventloopExtension = new EventloopExtension();

  @RegisterExtension
  static final InstrumentationExtension testing = HttpServerInstrumentationExtension.forAgent();

  private static final Eventloop eventloop = Reactor.getCurrentReactor();
  private Thread thread;

  @Override
  protected HttpServer setupServer() throws Exception {

    AsyncServlet captureHttpHeadersAsyncServlet =
        request -> {
          HttpResponse httpResponse =
              HttpResponse.builder()
                  .withBody(CAPTURE_HEADERS.getBody())
                  .withCode(CAPTURE_HEADERS.getStatus())
                  .withHeader(HttpHeaders.of(TEST_RESPONSE_HEADER), HttpHeaderValue.of("test"))
                  .build();
          controller(CAPTURE_HEADERS, () -> httpResponse);
          return httpResponse.toPromise();
        };
    AsyncServlet indexChildAsyncServlet =
        request -> {
          HttpResponse httpResponse =
              HttpResponse.builder()
                  .withBody(INDEXED_CHILD.getBody())
                  .withCode(INDEXED_CHILD.getStatus())
                  .build();
          INDEXED_CHILD.collectSpanAttributes(
              id ->
                  id.equals(ID_PARAMETER_NAME)
                      ? request.getQueryParameter(ID_PARAMETER_NAME)
                      : null);
          controller(
              INDEXED_CHILD,
              () -> {
                INDEXED_CHILD.collectSpanAttributes(
                    id ->
                        id.equals(ID_PARAMETER_NAME)
                            ? request.getQueryParameter(ID_PARAMETER_NAME)
                            : null);
                return httpResponse;
              });
          return httpResponse.toPromise();
        };

    RoutingServlet routingServlet =
        RoutingServlet.builder(eventloop)
            .with(GET, SUCCESS.getPath(), request -> prepareResponse(SUCCESS))
            .with(GET, QUERY_PARAM.getPath(), request -> prepareResponse(QUERY_PARAM))
            .with(GET, ERROR.getPath(), request -> prepareResponse(ERROR))
            .with(GET, NOT_FOUND.getPath(), request -> prepareResponse(NOT_FOUND))
            .with(GET, EXCEPTION.getPath(), request -> prepareResponse(EXCEPTION))
            .with(GET, REDIRECT.getPath(), request -> prepareResponse(REDIRECT))
            .with(GET, CAPTURE_HEADERS.getPath(), captureHttpHeadersAsyncServlet)
            .with(GET, INDEXED_CHILD.getPath(), indexChildAsyncServlet)
            .build();

    HttpServer server = HttpServer.builder(eventloop, routingServlet).withListenPort(port).build();

    server.listen();
    thread = new Thread(eventloop);
    thread.start();
    return server;
  }

  @Override
  protected void stopServer(HttpServer server) throws Exception {
    server.closeFuture().get();
    thread.join();
  }

  @Override
  protected void configure(HttpServerTestOptions options) {
    options.setTestHttpPipelining(false);
    options.setTestRedirect(false);
    options.setTestException(false);
    options.disableTestNonStandardHttpMethod();
    options.setExpectedServerSpanNameMapper(
        (uri, method, route) -> {
          if (HttpConstants._OTHER.equals(method)) {
            method = "HTTP";
          }
          return method;
        });
    options.setHttpAttributes(endpoint -> ImmutableSet.of(NETWORK_PEER_PORT));
  }

  Promise<HttpResponse> prepareResponse(ServerEndpoint endpoint) {
    HttpResponse httpResponse =
        HttpResponse.builder().withBody(endpoint.getBody()).withCode(endpoint.getStatus()).build();
    controller(endpoint, () -> httpResponse);
    return httpResponse.toPromise();
  }
}
