/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.v5_0;

import static io.opentelemetry.javaagent.instrumentation.apachehttpclient.commons.BytesTransferMetrics.createOrGetWithParentContext;

import io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.instrumentation.apachehttpclient.commons.BytesTransferMetrics;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.nio.AsyncResponseConsumer;
import org.apache.hc.core5.http.nio.CapacityChannel;
import org.apache.hc.core5.http.protocol.HttpContext;

public final class WrappedResponseConsumer<T> implements AsyncResponseConsumer<T> {
  private final AsyncResponseConsumer<T> delegate;
  private final Context parentContext;

  public WrappedResponseConsumer(Context parentContext, AsyncResponseConsumer<T> delegate) {
    this.parentContext = parentContext;
    this.delegate = delegate;
  }

  @Override
  public void consumeResponse(
      HttpResponse httpResponse,
      EntityDetails entityDetails,
      HttpContext httpContext,
      FutureCallback<T> futureCallback)
      throws HttpException, IOException {
    delegate.consumeResponse(httpResponse, entityDetails, httpContext, futureCallback);
  }

  @Override
  public void informationResponse(HttpResponse httpResponse, HttpContext httpContext)
      throws HttpException, IOException {
    delegate.informationResponse(httpResponse, httpContext);
  }

  @Override
  public void failed(Exception e) {
    delegate.failed(e);
  }

  @Override
  public void updateCapacity(CapacityChannel capacityChannel) throws IOException {
    delegate.updateCapacity(capacityChannel);
  }

  @Override
  public void consume(ByteBuffer byteBuffer) throws IOException {
    if (byteBuffer.hasRemaining()) {
      BytesTransferMetrics metrics = createOrGetWithParentContext(parentContext);
      metrics.addResponseBytes(byteBuffer.limit());
    }
    delegate.consume(byteBuffer);
  }

  @Override
  public void streamEnd(List<? extends Header> list) throws HttpException, IOException {
    delegate.streamEnd(list);
  }

  @Override
  public void releaseResources() {
    delegate.releaseResources();
  }
}
