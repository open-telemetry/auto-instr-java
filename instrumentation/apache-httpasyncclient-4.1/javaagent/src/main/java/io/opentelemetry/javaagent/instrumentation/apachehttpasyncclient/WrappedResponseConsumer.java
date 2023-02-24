/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpasyncclient;

import io.opentelemetry.context.Context;
import java.io.IOException;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.nio.ContentDecoder;
import org.apache.http.nio.IOControl;
import org.apache.http.nio.protocol.HttpAsyncResponseConsumer;
import org.apache.http.protocol.HttpContext;

public final class WrappedResponseConsumer<T> implements HttpAsyncResponseConsumer<T> {
  private final Context parentContext;
  private final HttpAsyncResponseConsumer<T> delegate;

  public WrappedResponseConsumer(Context parentContext, HttpAsyncResponseConsumer<T> delegate) {
    this.parentContext = parentContext;
    this.delegate = delegate;
  }

  @Override
  public void responseReceived(HttpResponse httpResponse) throws IOException, HttpException {
    delegate.responseReceived(httpResponse);
  }

  @Override
  public void consumeContent(ContentDecoder contentDecoder, IOControl ioControl)
      throws IOException {
    delegate.consumeContent(new WrappedContentDecoder(parentContext, contentDecoder), ioControl);
  }

  @Override
  public void responseCompleted(HttpContext httpContext) {
    delegate.responseCompleted(httpContext);
  }

  @Override
  public void failed(Exception e) {
    delegate.failed(e);
  }

  @Override
  public Exception getException() {
    return delegate.getException();
  }

  @Override
  public T getResult() {
    return delegate.getResult();
  }

  @Override
  public boolean isDone() {
    return delegate.isDone();
  }

  @Override
  public void close() throws IOException {
    delegate.close();
  }

  @Override
  public boolean cancel() {
    return delegate.cancel();
  }
}
