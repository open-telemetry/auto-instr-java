/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpasyncclient;

import static io.opentelemetry.javaagent.instrumentation.apachehttpasyncclient.ApacheHttpAsyncClientSingletons.helper;

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
  private final HttpAsyncRequestProducer delegate;
  private final WrappedFutureCallback<?> wrappedFutureCallback;

  public WrappedRequestProducer(
      Context parentContext,
      HttpAsyncRequestProducer delegate,
      WrappedFutureCallback<?> wrappedFutureCallback) {
    this.parentContext = parentContext;
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

    ApacheHttpClientRequest otelRequest = new CustomApacheHttpClientRequest(target, request);
    Context context = helper().startInstrumentation(parentContext, otelRequest);

    if (context != null) {
      wrappedFutureCallback.context = context;
      wrappedFutureCallback.otelRequest = otelRequest;
    }

    return request;
  }

  @Override
  public void produceContent(ContentEncoder encoder, IOControl ioctrl) throws IOException {
    delegate.produceContent(encoder, ioctrl);
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
