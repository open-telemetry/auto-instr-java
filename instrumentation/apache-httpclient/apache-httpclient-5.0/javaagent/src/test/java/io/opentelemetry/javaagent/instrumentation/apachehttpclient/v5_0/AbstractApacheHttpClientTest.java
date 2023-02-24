/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.v5_0;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.instrumentation.testing.junit.http.AbstractHttpClientTest;
import io.opentelemetry.instrumentation.testing.junit.http.HttpClientResult;
import io.opentelemetry.instrumentation.testing.junit.http.HttpClientTestOptions;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.net.URI;
import java.time.Duration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.message.BasicHeader;
import org.apache.hc.core5.http.protocol.BasicHttpContext;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.util.Timeout;

abstract class AbstractApacheHttpClientTest<T extends HttpRequest>
    extends AbstractHttpClientTest<T> {

  @Override
  protected String userAgent() {
    return "apachehttpclient";
  }

  @Override
  protected void configure(HttpClientTestOptions.Builder optionsBuilder) {
    optionsBuilder.setUserAgent(userAgent());
    optionsBuilder.enableTestReadTimeout();
    optionsBuilder.setHttpAttributes(this::getHttpAttributes);
    optionsBuilder.setResponseCodeOnRedirectError(302);
  }

  protected Set<AttributeKey<?>> getHttpAttributes(URI endpoint) {
    Set<AttributeKey<?>> attributes = new HashSet<>();
    attributes.add(SemanticAttributes.NET_PEER_NAME);
    attributes.add(SemanticAttributes.NET_PEER_PORT);
    attributes.add(SemanticAttributes.HTTP_URL);
    attributes.add(SemanticAttributes.HTTP_METHOD);
    attributes.add(SemanticAttributes.HTTP_REQUEST_CONTENT_LENGTH);
    attributes.add(SemanticAttributes.HTTP_RESPONSE_CONTENT_LENGTH);
    if (endpoint.toString().contains("/success")) {
      attributes.add(SemanticAttributes.HTTP_FLAVOR);
    }
    attributes.add(SemanticAttributes.HTTP_USER_AGENT);
    return attributes;
  }

  @Override
  public T buildRequest(String method, URI uri, Map<String, String> headers) {
    T request = createRequest(method, uri);
    request.addHeader("user-agent", userAgent());
    headers.forEach((key, value) -> request.setHeader(new BasicHeader(key, value)));
    return request;
  }

  @Override
  public int sendRequest(T request, String method, URI uri, Map<String, String> headers)
      throws Exception {
    return getResponseCode(executeRequest(request, uri));
  }

  @Override
  public void sendRequestWithCallback(
      T request,
      String method,
      URI uri,
      Map<String, String> headers,
      HttpClientResult httpClientResult) {
    try {
      executeRequestWithCallback(request, uri, httpClientResult);
    } catch (Throwable throwable) {
      httpClientResult.complete(throwable);
    }
  }

  protected HttpHost getHost(URI uri) {
    return new HttpHost(uri.getScheme(), uri.getHost(), uri.getPort());
  }

  protected HttpContext getContext() {
    return new BasicHttpContext();
  }

  protected static String fullPathFromUri(URI uri) {
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

  abstract T createRequest(String method, URI uri);

  abstract HttpResponse executeRequest(T request, URI uri) throws Exception;

  abstract void executeRequestWithCallback(T request, URI uri, HttpClientResult httpClientResult)
      throws Exception;

  private static int getResponseCode(HttpResponse response) {
    return response.getCode();
  }

  // Apache HttpClient 5.2 introduced Timeout#of(Duration), which causes errorprone failure when
  // running testLatestDeps
  @SuppressWarnings("PreferJavaTimeOverload")
  static Timeout getTimeout(Duration duration) {
    return Timeout.of(duration.toMillis(), TimeUnit.MILLISECONDS);
  }
}
