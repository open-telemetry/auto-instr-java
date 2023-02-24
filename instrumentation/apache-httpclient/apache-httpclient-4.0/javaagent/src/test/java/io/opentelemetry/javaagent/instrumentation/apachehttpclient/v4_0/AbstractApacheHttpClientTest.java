/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.v4_0;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.instrumentation.testing.junit.http.AbstractHttpClientTest;
import io.opentelemetry.instrumentation.testing.junit.http.HttpClientResult;
import io.opentelemetry.instrumentation.testing.junit.http.HttpClientTestOptions;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.net.URI;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

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
    optionsBuilder.setHttpAttributes(AbstractApacheHttpClientTest::getHttpAttributes);
    optionsBuilder.setResponseCodeOnRedirectError(302);
  }

  private static Set<AttributeKey<?>> getHttpAttributes(URI endpoint) {
    Set<AttributeKey<?>> attributes = new HashSet<>(HttpClientTestOptions.DEFAULT_HTTP_ATTRIBUTES);
    attributes.add(SemanticAttributes.HTTP_REQUEST_CONTENT_LENGTH);
    attributes.add(SemanticAttributes.HTTP_RESPONSE_CONTENT_LENGTH);
    return attributes;
  }

  @Override
  public T buildRequest(String method, URI uri, Map<String, String> headers) {
    T request = createRequest(method, uri);
    request.addHeader("user-agent", userAgent());
    headers.forEach(request::setHeader);
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
      HttpClientResult requestResult) {
    try {
      executeRequestWithCallback(request, uri, requestResult);
    } catch (Throwable throwable) {
      requestResult.complete(throwable);
    }
  }

  protected HttpHost getHost(URI uri) {
    return new HttpHost(uri.getHost(), uri.getPort(), uri.getScheme());
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

  abstract void executeRequestWithCallback(T request, URI uri, HttpClientResult requestResult)
      throws Exception;

  private static int getResponseCode(HttpResponse response) {
    return response.getStatusLine().getStatusCode();
  }
}
