/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpasyncclient;

import static io.opentelemetry.javaagent.instrumentation.apachehttpasyncclient.ApacheHttpAsyncClientSingletons.helper;
import static io.opentelemetry.javaagent.instrumentation.apachehttpclient.v4_0.commons.ApacheHttpClientContextManager.httpContextManager;

import io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.instrumentation.apachehttpclient.v4_0.commons.ApacheHttpClientRequest;
import java.io.IOException;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.nio.ContentEncoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.protocol.HttpAsyncRequestProducer;
import org.apache.http.protocol.HttpContext;

public final class WrappedRequestProducer implements HttpAsyncRequestProducer {
  private final Context parentContext;
  private final HttpContext httpContext;
  private final HttpAsyncRequestProducer delegate;
  private final WrappedFutureCallback<?> wrappedFutureCallback;

  public WrappedRequestProducer(
      Context parentContext,
      HttpContext httpContext,
      HttpAsyncRequestProducer delegate,
      WrappedFutureCallback<?> wrappedFutureCallback) {
    this.parentContext = parentContext;
    this.httpContext = httpContext;
    this.delegate = delegate;
    this.wrappedFutureCallback = wrappedFutureCallback;
  }

  @Override
  public HttpHost getTarget() {
    return delegate.getTarget();
  }

  @Override
  public HttpRequest generateRequest() throws IOException, HttpException {
    HttpHost target = delegate.getTarget();
    HttpRequest request = delegate.generateRequest();

    ApacheHttpClientRequest otelRequest;
    otelRequest = new ApacheHttpClientRequest(parentContext, target, request);
    Context context = helper().startInstrumentation(parentContext, request, otelRequest);

    if (context != null) {
      wrappedFutureCallback.context = context;
      wrappedFutureCallback.otelRequest = otelRequest;

      // As the http processor instrumentation is going to be called asynchronously,
      // we will need to store the otel context variables in http context for the
      // http processor instrumentation to use
      httpContextManager().setCurrentContext(httpContext, context);
    }

    return request;
  }

  @Override
  public void produceContent(ContentEncoder encoder, IOControl ioctrl) throws IOException {
    delegate.produceContent(new WrappedContentEncoder(parentContext, encoder), ioctrl);
  }

  @Override
  public void requestCompleted(HttpContext context) {
    delegate.requestCompleted(context);
  }

  @Override
  public void failed(Exception ex) {
    delegate.failed(ex);
  }

  @Override
  public boolean isRepeatable() {
    return delegate.isRepeatable();
  }

  @Override
  public void resetRequest() throws IOException {
    delegate.resetRequest();
  }

  @Override
  public void close() throws IOException {
    delegate.close();
  }
}
