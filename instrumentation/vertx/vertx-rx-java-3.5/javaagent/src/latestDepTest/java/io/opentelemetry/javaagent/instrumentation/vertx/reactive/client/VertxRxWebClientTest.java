/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.vertx.reactive.client;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.http.AbstractHttpClientTest;
import io.opentelemetry.instrumentation.testing.junit.http.HttpClientInstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.http.HttpClientResult;
import io.opentelemetry.instrumentation.testing.junit.http.HttpClientTestOptions;
import io.reactivex.functions.Consumer;
import io.vertx.core.VertxOptions;
import io.vertx.core.http.HttpMethod;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.reactivex.core.Vertx;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.ext.web.client.HttpRequest;
import io.vertx.reactivex.ext.web.client.HttpResponse;
import io.vertx.reactivex.ext.web.client.WebClient;
import java.net.URI;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.extension.RegisterExtension;

class VertxRxWebClientTest extends AbstractHttpClientTest<HttpRequest<Buffer>> {

  @RegisterExtension
  static final InstrumentationExtension testing = HttpClientInstrumentationExtension.forAgent();

  private final WebClient httpClient = buildClient();

  private static WebClient buildClient() {
    Vertx vertx = Vertx.vertx(new VertxOptions());
    WebClientOptions clientOptions =
        new WebClientOptions().setConnectTimeout(Math.toIntExact(CONNECTION_TIMEOUT.toMillis()));
    return WebClient.create(vertx, clientOptions);
  }

  @Override
  public HttpRequest<Buffer> buildRequest(String method, URI uri, Map<String, String> headers) {
    HttpRequest<Buffer> request = httpClient.requestAbs(HttpMethod.valueOf(method), uri.toString());
    headers.forEach(request::putHeader);
    return request;
  }

  @Override
  public int sendRequest(
      HttpRequest<Buffer> request, String method, URI uri, Map<String, String> headers) {
    return request.rxSend().blockingGet().statusCode();
  }

  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Override
  public void sendRequestWithCallback(
      HttpRequest<Buffer> request,
      String method,
      URI uri,
      Map<String, String> headers,
      HttpClientResult requestResult) {
    request
        .rxSend()
        .subscribe(
            (Consumer<HttpResponse<?>>)
                httpResponse -> requestResult.complete(httpResponse.statusCode()),
            requestResult::complete);
  }

  @Override
  protected void configure(HttpClientTestOptions.Builder optionsBuilder) {
    optionsBuilder.disableTestRedirects();
    optionsBuilder.disableTestHttps();
    optionsBuilder.disableTestReadTimeout();
    optionsBuilder.disableTestNonStandardHttpMethod();
    optionsBuilder.setHttpAttributes(VertxRxWebClientTest::getHttpAttributes);
    optionsBuilder.setClientSpanErrorMapper(VertxRxWebClientTest::clientSpanError);
    optionsBuilder.setExpectedClientSpanNameMapper(VertxRxWebClientTest::expectedClientSpanName);
    optionsBuilder.setSingleConnectionFactory(VertxRxSingleConnection::new);
  }

  private static Set<AttributeKey<?>> getHttpAttributes(URI uri) {
    switch (uri.toString()) {
      case "http://localhost:61/": // unopened port
      case "http://192.0.2.1/": // non routable address
        return Collections.emptySet();
      default:
        return HttpClientTestOptions.DEFAULT_HTTP_ATTRIBUTES;
    }
  }

  private static Throwable clientSpanError(URI uri, Throwable exception) {
    if (exception.getClass() == RuntimeException.class) {
      switch (uri.toString()) {
        case "http://localhost:61/": // unopened port
        case "http://192.0.2.1/": // non routable address
          exception = exception.getCause();
          break;
        default:
          break;
      }
    }
    return exception;
  }

  private static String expectedClientSpanName(URI uri, String method) {
    switch (uri.toString()) {
      case "http://localhost:61/": // unopened port
      case "http://192.0.2.1/": // non routable address
        return "CONNECT";
      default:
        return HttpClientTestOptions.DEFAULT_EXPECTED_CLIENT_SPAN_NAME_MAPPER.apply(uri, method);
    }
  }
}
