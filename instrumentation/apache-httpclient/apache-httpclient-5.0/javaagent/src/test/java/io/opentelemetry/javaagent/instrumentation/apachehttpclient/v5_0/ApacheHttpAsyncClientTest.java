/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.v5_0;

import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.http.HttpClientInstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.http.HttpClientResult;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.CancellationException;
import org.apache.hc.client5.http.async.methods.SimpleHttpRequest;
import org.apache.hc.client5.http.async.methods.SimpleHttpResponse;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.http2.HttpVersionPolicy;
import org.apache.hc.core5.io.CloseMode;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApacheHttpAsyncClientTest {
  @RegisterExtension
  static final InstrumentationExtension testing = HttpClientInstrumentationExtension.forAgent();

  private final CloseableHttpAsyncClient client = createClient();

  @BeforeAll
  void setUp() {
    client.start();
  }

  @AfterAll
  void tearDown() {
    client.close(CloseMode.GRACEFUL);
  }

  private static CloseableHttpAsyncClient createClient() {
    return HttpAsyncClients.custom().setVersionPolicy(HttpVersionPolicy.FORCE_HTTP_1).build();
  }

  @Nested
  class ApacheClientUriRequestTest extends AbstractTest {
    @Override
    SimpleHttpRequest createRequest(String method, URI uri) {
      return new SimpleHttpRequest(method, uri);
    }
  }

  @Nested
  class ApacheClientHostRequestTest extends AbstractTest {
    @Override
    SimpleHttpRequest createRequest(String method, URI uri) {
      return new SimpleHttpRequest(method, getHost(uri), fullPathFromUri(uri));
    }
  }

  @Nested
  class ApacheClientHostAbsoluteUriRequestTest extends AbstractTest {
    @Override
    SimpleHttpRequest createRequest(String method, URI uri) {
      return new SimpleHttpRequest(method, URI.create(uri.toString()));
    }
  }

  @Nested
  class ApacheClientNullContextTest extends AbstractTest {
    @Override
    SimpleHttpRequest createRequest(String method, URI uri) {
      return new SimpleHttpRequest(method, uri);
    }

    @Override
    protected HttpContext getContext() {
      return null;
    }
  }

  abstract class AbstractTest extends AbstractApacheHttpClientTest<SimpleHttpRequest> {
    @Override
    public SimpleHttpRequest buildRequest(String method, URI uri, Map<String, String> headers) {
      SimpleHttpRequest httpRequest = super.buildRequest(method, uri, headers);
      RequestConfig.Builder configBuilder = RequestConfig.custom();
      configBuilder.setConnectTimeout(getTimeout(connectTimeout()));
      if (uri.toString().contains("/read-timeout")) {
        configBuilder.setResponseTimeout(getTimeout(readTimeout()));
      }
      RequestConfig requestConfig = configBuilder.build();
      httpRequest.setConfig(requestConfig);
      return httpRequest;
    }

    @Override
    HttpResponse executeRequest(SimpleHttpRequest request, URI uri) throws Exception {
      return client.execute(request, getContext(), null).get();
    }

    @Override
    void executeRequestWithCallback(SimpleHttpRequest request, URI uri, HttpClientResult result) {
      client.execute(request, getContext(), new ResponseCallback(result));
    }
  }

  private static class ResponseCallback implements FutureCallback<SimpleHttpResponse> {
    private final HttpClientResult httpClientResult;

    public ResponseCallback(HttpClientResult httpClientResult) {
      this.httpClientResult = httpClientResult;
    }

    @Override
    public void completed(SimpleHttpResponse response) {
      httpClientResult.complete(response.getCode());
    }

    @Override
    public void failed(Exception ex) {
      httpClientResult.complete(ex);
    }

    @Override
    public void cancelled() {
      httpClientResult.complete(new CancellationException());
    }
  }
}
