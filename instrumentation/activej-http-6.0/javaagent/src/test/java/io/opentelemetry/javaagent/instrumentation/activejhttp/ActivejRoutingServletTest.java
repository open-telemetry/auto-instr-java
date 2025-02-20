/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.activejhttp;

import static io.activej.http.HttpMethod.GET;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_REQUEST_METHOD;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_RESPONSE_STATUS_CODE;
import static io.opentelemetry.semconv.UrlAttributes.URL_PATH;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.activej.eventloop.Eventloop;
import io.activej.http.AsyncServlet;
import io.activej.http.HttpError;
import io.activej.http.HttpRequest;
import io.activej.http.HttpResponse;
import io.activej.http.RoutingServlet;
import io.activej.http.UrlParser;
import io.activej.promise.Promise;
import io.activej.reactor.Reactor;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import org.junit.ClassRule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

public class ActivejRoutingServletTest {

  @ClassRule public static final EventloopRule eventloopRule = new EventloopRule();

  @RegisterExtension
  static final InstrumentationExtension testing = AgentInstrumentationExtension.create();

  private static final Eventloop eventloop = Reactor.getCurrentReactor();

  @Test
  void testRequestSuccessFlow() throws Exception {
    AsyncServlet asyncServlet =
        request -> HttpResponse.ofCode(200).withBody("".getBytes(UTF_8)).toPromise();

    RoutingServlet routingServlet =
        RoutingServlet.builder(eventloop).with(GET, "/success", asyncServlet).build();

    String url = "http://some-test.com/success";
    HttpRequest httpRequest = HttpRequest.get(url).build();
    check(routingServlet.serve(httpRequest), "", 200);

    UrlParser urlParser = UrlParser.of(url);

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("GET")
                        .hasNoParent()
                        .hasKind(SpanKind.SERVER)
                        .hasAttributesSatisfying(
                            equalTo(URL_PATH, urlParser.getPath()),
                            equalTo(HTTP_REQUEST_METHOD, "GET"),
                            equalTo(HTTP_RESPONSE_STATUS_CODE, 200))));
  }

  @Test
  void testRequestNotFoundFlow() throws Exception {

    AsyncServlet asyncServlet = request -> HttpResponse.notFound404().withBody("").toPromise();

    RoutingServlet routingServlet =
        RoutingServlet.builder(eventloop).with(GET, "/notfound", asyncServlet).build();

    String url = "http://some-test.com/notfound";
    HttpRequest httpRequest = HttpRequest.get(url).build();
    check(routingServlet.serve(httpRequest), "", 404);

    UrlParser urlParser = UrlParser.of(url);

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("GET")
                        .hasNoParent()
                        .hasKind(SpanKind.SERVER)
                        .hasAttributesSatisfying(
                            equalTo(URL_PATH, urlParser.getPath()),
                            equalTo(HTTP_REQUEST_METHOD, "GET"),
                            equalTo(HTTP_RESPONSE_STATUS_CODE, 404))));
  }

  private static void check(Promise<HttpResponse> promise, String expectedBody, int expectedCode) {
    assertTrue(promise.isComplete());
    if (promise.isResult() && !promise.isException()) {
      HttpResponse result = promise.getResult();
      assertEquals(expectedBody, result.getBody().asString(UTF_8));
      assertEquals(expectedCode, result.getCode());
    } else {
      assertEquals(expectedCode, ((HttpError) promise.getException()).getCode());
    }
  }
}
