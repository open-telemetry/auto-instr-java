/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.v5_0;

import io.opentelemetry.context.Context;
import java.io.IOException;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.nio.AsyncRequestProducer;
import org.apache.hc.core5.http.nio.DataStreamChannel;
import org.apache.hc.core5.http.nio.RequestChannel;
import org.apache.hc.core5.http.protocol.HttpContext;

public final class WrappedRequestProducer implements AsyncRequestProducer {
  private final Context parentContext;
  private final AsyncRequestProducer delegate;
  private final WrappedFutureCallback<?> callback;

  public WrappedRequestProducer(
      Context parentContext, AsyncRequestProducer delegate, WrappedFutureCallback<?> callback) {
    this.parentContext = parentContext;
    this.delegate = delegate;
    this.callback = callback;
  }

  @Override
  public void failed(Exception ex) {
    delegate.failed(ex);
  }

  @Override
  public void sendRequest(RequestChannel channel, HttpContext context)
      throws HttpException, IOException {
    RequestChannel requestChannel;
    requestChannel = new WrappedRequestChannel(channel, parentContext, callback);
    delegate.sendRequest(requestChannel, context);
  }

  @Override
  public boolean isRepeatable() {
    return delegate.isRepeatable();
  }

  @Override
  public int available() {
    return delegate.available();
  }

  @Override
  public void produce(DataStreamChannel channel) throws IOException {
    delegate.produce(channel);
  }

  @Override
  public void releaseResources() {
    delegate.releaseResources();
  }
}
