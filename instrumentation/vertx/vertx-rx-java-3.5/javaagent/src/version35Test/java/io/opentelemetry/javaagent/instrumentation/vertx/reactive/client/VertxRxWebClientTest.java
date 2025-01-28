/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.vertx.reactive.client;

import static io.opentelemetry.semconv.NetworkAttributes.NETWORK_PROTOCOL_VERSION;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_ADDRESS;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_PORT;

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
import java.util.HashSet;
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
    optionsBuilder.setSingleConnectionFactory(VertxRxSingleConnection::new);
  }

  private static Set<AttributeKey<?>> getHttpAttributes(URI uri) {
    Set<AttributeKey<?>> attributes = new HashSet<>(HttpClientTestOptions.DEFAULT_HTTP_ATTRIBUTES);
    attributes.remove(NETWORK_PROTOCOL_VERSION);
    attributes.remove(SERVER_ADDRESS);
    attributes.remove(SERVER_PORT);
    return attributes;
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
}
