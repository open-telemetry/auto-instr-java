/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.playws;

import io.opentelemetry.instrumentation.testing.junit.http.HttpClientResult;
import java.io.IOException;
import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import play.libs.ws.StandaloneWSClient;
import play.libs.ws.StandaloneWSRequest;
import play.libs.ws.ahc.StandaloneAhcWSClient;

public class PlayJavaWsClientBaseTest extends PlayWsClientBaseTest<StandaloneWSRequest> {

  private static StandaloneWSClient wsClient;
  private static StandaloneWSClient wsClientWithReadTimeout;

  @BeforeEach
  void setup() {
    super.setup();
    wsClient = new StandaloneAhcWSClient(asyncHttpClient, materializer);
    wsClientWithReadTimeout =
        new StandaloneAhcWSClient(asyncHttpClientWithReadTimeout, materializer);
    autoCleanup.deferCleanup(wsClient);
    autoCleanup.deferCleanup(wsClientWithReadTimeout);
  }

  @AfterEach
  void tearDown() throws IOException {
    if (wsClient != null) {
      wsClient.close();
    }
    if (wsClientWithReadTimeout != null) {
      wsClientWithReadTimeout.close();
    }
    super.tearDown();
  }

  @Override
  public StandaloneWSRequest buildRequest(String method, URI uri, Map<String, String> headers) {
    StandaloneWSRequest request = getClient(uri).url(uri.toString()).setFollowRedirects(true);
    headers.forEach(request::addHeader);
    return request.setMethod(method);
  }

  @Override
  public int sendRequest(
      StandaloneWSRequest request, String method, URI uri, Map<String, String> headers)
      throws Exception {
    return request.execute().toCompletableFuture().get().getStatus();
  }

  @Override
  public void sendRequestWithCallback(
      StandaloneWSRequest request,
      String method,
      URI uri,
      Map<String, String> headers,
      HttpClientResult requestResult) {
    request
        .execute()
        .whenComplete(
            (response, throwable) -> {
              if (throwable != null) {
                requestResult.complete(throwable);
              } else {
                requestResult.complete(response.getStatus());
              }
            });
  }

  private StandaloneWSClient getClient(URI uri) {
    if (uri.toString().contains("/read-timeout")) {
      return wsClientWithReadTimeout;
    }
    return wsClient;
  }
}
