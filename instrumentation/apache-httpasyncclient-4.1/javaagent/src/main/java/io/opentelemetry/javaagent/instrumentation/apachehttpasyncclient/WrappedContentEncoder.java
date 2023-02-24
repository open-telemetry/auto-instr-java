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
import org.apache.http.nio.ContentEncoder;

public final class WrappedContentEncoder implements ContentEncoder {
  private final Context parentContext;
  private final ContentEncoder delegate;

  public WrappedContentEncoder(Context parentContext, ContentEncoder delegate) {
    this.parentContext = parentContext;
    this.delegate = delegate;
  }

  @Override
  public int write(ByteBuffer byteBuffer) throws IOException {
    BytesTransferMetrics metrics = createOrGetWithParentContext(parentContext);
    metrics.addRequestBytes(byteBuffer.limit());
    return delegate.write(byteBuffer);
  }

  @Override
  public void complete() throws IOException {
    delegate.complete();
  }

  @Override
  public boolean isCompleted() {
    return delegate.isCompleted();
  }
}
