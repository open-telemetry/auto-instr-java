/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.v4_0;

import static io.opentelemetry.instrumentation.testing.junit.http.AbstractHttpClientTest.CONNECTION_TIMEOUT;
import static io.opentelemetry.instrumentation.testing.junit.http.AbstractHttpClientTest.READ_TIMEOUT;

import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.http.HttpClientInstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.http.HttpClientResult;
import java.io.IOException;
import java.net.URI;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.params.ClientPNames;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.ClientConnectionManagerFactory;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicHttpRequest;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class ApacheHttpClientTest {
  @RegisterExtension
  static final InstrumentationExtension testing = HttpClientInstrumentationExtension.forAgent();

  private final DefaultHttpClient client = buildClient(false);
  private final DefaultHttpClient clientWithReadTimeout = buildClient(true);

  @AfterAll
  void tearDown() {
    client.getConnectionManager().shutdown();
    clientWithReadTimeout.getConnectionManager().shutdown();
  }

  private static DefaultHttpClient buildClient(boolean readTimeout) {
    HttpParams httpParams = new BasicHttpParams();
    HttpConnectionParams.setConnectionTimeout(httpParams, (int) CONNECTION_TIMEOUT.toMillis());
    if (readTimeout) {
      HttpConnectionParams.setSoTimeout(httpParams, (int) READ_TIMEOUT.toMillis());
    }
    httpParams.setParameter(
        ClientPNames.CONNECTION_MANAGER_FACTORY_CLASS_NAME,
        ThreadSafeClientConnManagerFactory.class.getName());
    return new DefaultHttpClient(httpParams);
  }

  private DefaultHttpClient getClient(URI uri) {
    if (uri.toString().contains("/read-timeout")) {
      return clientWithReadTimeout;
    }
    return client;
  }

  public static class ThreadSafeClientConnManagerFactory implements ClientConnectionManagerFactory {
    @Override
    public ClientConnectionManager newInstance(
        HttpParams httpParams, SchemeRegistry schemeRegistry) {
      return new ThreadSafeClientConnManager(schemeRegistry);
    }
  }

  @Nested
  class ApacheClientHostRequestTest extends AbstractTest<BasicHttpRequest> {
    @Override
    public BasicHttpRequest createRequest(String method, URI uri) {
      // also testing with an absolute path below
      return new BasicHttpRequest(method, fullPathFromUri(uri));
    }

    @Override
    public HttpResponse doExecuteRequest(BasicHttpRequest request, URI uri) throws Exception {
      return getClient(uri).execute(getHost(uri), request);
    }

    @Override
    void executeRequestWithCallback(BasicHttpRequest request, URI uri, HttpClientResult result)
        throws Exception {
      getClient(uri).execute(getHost(uri), request, new HttpResponseHandler(result));
    }
  }

  @Nested
  class ApacheClientHostAbsoluteUriRequestTest extends AbstractTest<BasicHttpRequest> {
    @Override
    BasicHttpRequest createRequest(String method, URI uri) {
      return new BasicHttpRequest(method, uri.toString());
    }

    @Override
    HttpResponse doExecuteRequest(BasicHttpRequest request, URI uri) throws Exception {
      return getClient(uri).execute(getHost(uri), request);
    }

    @Override
    void executeRequestWithCallback(BasicHttpRequest request, URI uri, HttpClientResult result)
        throws Exception {
      getClient(uri).execute(getHost(uri), request, new HttpResponseHandler(result));
    }
  }

  @Nested
  class ApacheClientHostRequestContextTest extends AbstractTest<BasicHttpRequest> {
    @Override
    BasicHttpRequest createRequest(String method, URI uri) {
      // also testing with an absolute path below
      return new BasicHttpRequest(method, fullPathFromUri(uri));
    }

    @Override
    HttpResponse doExecuteRequest(BasicHttpRequest request, URI uri) throws Exception {
      return getClient(uri).execute(getHost(uri), request, getContext());
    }

    @Override
    void executeRequestWithCallback(BasicHttpRequest request, URI uri, HttpClientResult result)
        throws Exception {
      getClient(uri).execute(getHost(uri), request, new HttpResponseHandler(result), getContext());
    }
  }

  @Nested
  class ApacheClientHostAbsoluteUriRequestContextTest extends AbstractTest<BasicHttpRequest> {
    @Override
    BasicHttpRequest createRequest(String method, URI uri) {
      return new BasicHttpRequest(method, uri.toString());
    }

    @Override
    HttpResponse doExecuteRequest(BasicHttpRequest request, URI uri) throws Exception {
      return getClient(uri).execute(getHost(uri), request, getContext());
    }

    @Override
    void executeRequestWithCallback(BasicHttpRequest request, URI uri, HttpClientResult result)
        throws Exception {
      getClient(uri).execute(getHost(uri), request, new HttpResponseHandler(result), getContext());
    }
  }

  @Nested
  class ApacheClientUriRequestTest extends AbstractTest<HttpUriRequest> {
    @Override
    HttpUriRequest createRequest(String method, URI uri) {
      return new HttpUriRequest(method, uri);
    }

    @Override
    HttpResponse doExecuteRequest(HttpUriRequest request, URI uri) throws Exception {
      return getClient(uri).execute(request);
    }

    @Override
    void executeRequestWithCallback(HttpUriRequest request, URI uri, HttpClientResult result)
        throws Exception {
      getClient(uri).execute(request, new HttpResponseHandler(result));
    }
  }

  @Nested
  class ApacheClientUriRequestContextTest extends AbstractTest<HttpUriRequest> {
    @Override
    HttpUriRequest createRequest(String method, URI uri) {
      return new HttpUriRequest(method, uri);
    }

    @Override
    HttpResponse doExecuteRequest(HttpUriRequest request, URI uri) throws Exception {
      return getClient(uri).execute(request, getContext());
    }

    @Override
    void executeRequestWithCallback(HttpUriRequest request, URI uri, HttpClientResult result)
        throws Exception {
      getClient(uri).execute(request, new HttpResponseHandler(result), getContext());
    }
  }

  abstract static class AbstractTest<T extends HttpRequest>
      extends AbstractApacheHttpClientTest<T> {
    @Override
    final HttpResponse executeRequest(T request, URI uri) throws Exception {
      HttpResponse httpResponse = doExecuteRequest(request, uri);
      httpResponse.getEntity().getContent().close();
      return httpResponse;
    }

    abstract HttpResponse doExecuteRequest(T request, URI uri) throws Exception;
  }

  private static class HttpResponseHandler implements ResponseHandler<Void> {
    private final HttpClientResult requestResult;

    public HttpResponseHandler(HttpClientResult requestResult) {
      this.requestResult = requestResult;
    }

    @Override
    public Void handleResponse(HttpResponse response) throws IOException {
      response.getEntity().getContent().close();
      requestResult.complete(response.getStatusLine().getStatusCode());
      return null;
    }
  }
}
