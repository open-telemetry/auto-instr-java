/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpasyncclient;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.http.AbstractHttpClientTest;
import io.opentelemetry.instrumentation.testing.junit.http.HttpClientInstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.http.HttpClientResult;
import io.opentelemetry.instrumentation.testing.junit.http.HttpClientTestOptions;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.concurrent.FutureCallback;
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient;
import org.apache.http.impl.nio.client.HttpAsyncClients;
import org.apache.http.message.BasicHeader;
import org.apache.http.nio.client.HttpAsyncClient;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApacheHttpAsyncClientTest {

  @RegisterExtension
  static final InstrumentationExtension testing = HttpClientInstrumentationExtension.forAgent();

  private final RequestConfig requestConfig =
      RequestConfig.custom()
          .setConnectTimeout((int) AbstractHttpClientTest.CONNECTION_TIMEOUT.toMillis())
          .build();

  private final RequestConfig requestWithReadTimeoutConfig =
      RequestConfig.copy(requestConfig)
          .setSocketTimeout((int) AbstractHttpClientTest.READ_TIMEOUT.toMillis())
          .build();

  private final CloseableHttpAsyncClient client =
      HttpAsyncClients.custom().setDefaultRequestConfig(requestConfig).build();
  private final CloseableHttpAsyncClient clientWithReadTimeout =
      HttpAsyncClients.custom().setDefaultRequestConfig(requestWithReadTimeoutConfig).build();

  @BeforeAll
  void setUp() {
    client.start();
    clientWithReadTimeout.start();
  }

  @AfterAll
  void tearDown() throws Exception {
    client.close();
    clientWithReadTimeout.close();
  }

  HttpAsyncClient getClient(URI uri) {
    if (uri.toString().contains("/read-timeout")) {
      return clientWithReadTimeout;
    }
    return client;
  }

  @Nested
  class ApacheClientUriRequestTest extends AbstractHttpClientTest<HttpUriRequest> {

    @Override
    public HttpUriRequest buildRequest(String method, URI uri, Map<String, String> headers) {
      return configureRequest(new HttpUriRequest(method, uri), headers);
    }

    @Override
    public int sendRequest(
        HttpUriRequest request, String method, URI uri, Map<String, String> headers)
        throws Exception {
      return getResponseCode(getClient(uri).execute(request, null).get());
    }

    @Override
    public void sendRequestWithCallback(
        HttpUriRequest request,
        String method,
        URI uri,
        Map<String, String> headers,
        HttpClientResult httpClientResult) {
      getClient(uri).execute(request, responseCallback(httpClientResult));
    }

    @Override
    protected void configure(HttpClientTestOptions.Builder optionsBuilder) {
      configureTest(optionsBuilder);
    }
  }

  @Nested
  class ApacheClientHostRequestTest extends AbstractHttpClientTest<HttpUriRequest> {

    @Override
    public HttpUriRequest buildRequest(String method, URI uri, Map<String, String> headers) {
      return configureRequest(
          new HttpUriRequest(method, URI.create(fullPathFromUri(uri))), headers);
    }

    @Override
    public int sendRequest(
        HttpUriRequest request, String method, URI uri, Map<String, String> headers)
        throws Exception {
      return getResponseCode(
          getClient(uri)
              .execute(new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme()), request, null)
              .get());
    }

    @Override
    public void sendRequestWithCallback(
        HttpUriRequest request,
        String method,
        URI uri,
        Map<String, String> headers,
        HttpClientResult httpClientResult) {
      getClient(uri)
          .execute(
              new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme()),
              request,
              responseCallback(httpClientResult));
    }

    @Override
    protected void configure(HttpClientTestOptions.Builder optionsBuilder) {
      configureTest(optionsBuilder);
    }
  }

  @Nested
  class ApacheClientHostAbsoluteUriRequestTest extends AbstractHttpClientTest<HttpUriRequest> {

    @Override
    public HttpUriRequest buildRequest(String method, URI uri, Map<String, String> headers) {
      return configureRequest(new HttpUriRequest(method, URI.create(uri.toString())), headers);
    }

    @Override
    public int sendRequest(
        HttpUriRequest request, String method, URI uri, Map<String, String> headers)
        throws Exception {
      return getResponseCode(
          getClient(uri)
              .execute(new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme()), request, null)
              .get());
    }

    @Override
    public void sendRequestWithCallback(
        HttpUriRequest request,
        String method,
        URI uri,
        Map<String, String> headers,
        HttpClientResult httpClientResult) {
      getClient(uri)
          .execute(
              new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme()),
              request,
              responseCallback(httpClientResult));
    }

    @Override
    protected void configure(HttpClientTestOptions.Builder optionsBuilder) {
      configureTest(optionsBuilder);
    }
  }

  HttpUriRequest configureRequest(HttpUriRequest request, Map<String, String> headers) {
    request.addHeader("user-agent", "httpasyncclient");
    headers.forEach((key, value) -> request.setHeader(new BasicHeader(key, value)));
    return request;
  }

  int getResponseCode(HttpResponse response) {
    try {
      if (response.getEntity() != null && response.getEntity().getContent() != null) {
        response.getEntity().getContent().close();
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return response.getStatusLine().getStatusCode();
  }

  static FutureCallback<HttpResponse> responseCallback(HttpClientResult httpClientResult) {
    return new FutureCallback<HttpResponse>() {
      @Override
      public void completed(HttpResponse response) {
        try {
          if (response.getEntity() != null && response.getEntity().getContent() != null) {
            response.getEntity().getContent().close();
          }
        } catch (IOException e) {
          throw new UncheckedIOException(e);
        }
        httpClientResult.complete(response.getStatusLine().getStatusCode());
      }

      @Override
      public void failed(Exception e) {
        httpClientResult.complete(e);
      }

      @Override
      public void cancelled() {
        httpClientResult.complete(new CancellationException());
      }
    };
  }

  void configureTest(HttpClientTestOptions.Builder optionsBuilder) {
    optionsBuilder.setUserAgent("httpasyncclient");
    optionsBuilder.setResponseCodeOnRedirectError(302);
    optionsBuilder.enableTestReadTimeout();
    optionsBuilder.setHttpAttributes(ApacheHttpAsyncClientTest::getHttpAttributes);
  }

  private static Set<AttributeKey<?>> getHttpAttributes(URI endpoint) {
    Set<AttributeKey<?>> attributes = new HashSet<>(HttpClientTestOptions.DEFAULT_HTTP_ATTRIBUTES);
    attributes.add(SemanticAttributes.HTTP_SCHEME);
    attributes.add(SemanticAttributes.HTTP_TARGET);
    return attributes;
  }

  static String fullPathFromUri(URI uri) {
    StringBuilder builder = new StringBuilder();
    if (uri.getPath() != null) {
      builder.append(uri.getPath());
    }

    if (uri.getQuery() != null) {
      builder.append('?');
      builder.append(uri.getQuery());
    }

    if (uri.getFragment() != null) {
      builder.append('#');
      builder.append(uri.getFragment());
    }
    return builder.toString();
  }
}
