/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpasyncclient;

import static io.opentelemetry.javaagent.instrumentation.apachehttpclient.commons.BytesTransferMetrics.createOrGetWithParentContext;

import io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.instrumentation.apachehttpclient.commons.BytesTransferMetrics;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.apache.http.nio.ContentDecoder;

public final class WrappedContentDecoder implements ContentDecoder {
  private final Context parentContext;
  private final ContentDecoder delegate;

  public WrappedContentDecoder(Context parentContext, ContentDecoder delegate) {
    this.delegate = delegate;
    this.parentContext = parentContext;
  }

  @Override
  public int read(ByteBuffer byteBuffer) throws IOException {
    if (byteBuffer.hasRemaining()) {
      BytesTransferMetrics metrics = createOrGetWithParentContext(parentContext);
      metrics.addResponseBytes(byteBuffer.limit());
    }
    return delegate.read(byteBuffer);
  }

  @Override
  public boolean isCompleted() {
    return delegate.isCompleted();
  }
}
