/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package client

import io.opentelemetry.instrumentation.test.AgentTestTrait
import io.opentelemetry.instrumentation.test.base.HttpClientTest
import io.vertx.core.VertxOptions
import io.vertx.core.http.HttpMethod
import io.vertx.ext.web.client.WebClientOptions
import io.vertx.reactivex.core.Vertx
import io.vertx.reactivex.core.buffer.Buffer
import io.vertx.reactivex.ext.web.client.HttpRequest
import io.vertx.reactivex.ext.web.client.HttpResponse
import io.vertx.reactivex.ext.web.client.WebClient
import java.util.function.Consumer
import spock.lang.Shared

class VertxRxWebClientTest extends HttpClientTest implements AgentTestTrait {

  @Shared
  Vertx vertx = Vertx.vertx(new VertxOptions())
  @Shared
  def clientOptions = new WebClientOptions().setConnectTimeout(CONNECT_TIMEOUT_MS)
  @Shared
  WebClient client = WebClient.create(vertx, clientOptions)

  @Override
  int doRequest(String method, URI uri, Map<String, String> headers) {
    def request = buildRequest(method, uri, headers)
    return sendRequest(request)
  }

  @Override
  int doReusedRequest(String method, URI uri) {
    def request = buildRequest(method, uri, [:])
    sendRequest(request)
    return sendRequest(request)
  }

  @Override
  void doRequestWithCallback(String method, URI uri, Map<String, String> headers = [:], Consumer<Integer> callback) {
    def request = buildRequest(method, uri, headers)
    request.rxSend()
      .subscribe(new io.reactivex.functions.Consumer<HttpResponse<?>>() {
        @Override
        void accept(HttpResponse<?> httpResponse) throws Exception {
          callback.accept(httpResponse.statusCode())
        }
      })
  }

  private HttpRequest<Buffer> buildRequest(String method, URI uri, Map<String, String> headers) {
    def request = client.request(HttpMethod.valueOf(method), uri.port, uri.host, "$uri")
    headers.each { request.putHeader(it.key, it.value) }
    return request
  }

  private static int sendRequest(request) {
    return request.rxSend().blockingGet().statusCode()
  }

  @Override
  String userAgent() {
    return "Vert.x-WebClient"
  }

  @Override
  boolean testRedirects() {
    false
  }

  @Override
  boolean testConnectionFailure() {
    false
  }

  boolean testRemoteConnection() {
    // FIXME: figure out how to configure timeouts.
    false
  }

  @Override
  boolean testCausality() {
    false
  }

  @Override
  boolean testCallbackWithParent() {
    //Make rxjava2 instrumentation work with vert.x reactive in order to fix this test
    return false
  }
}
