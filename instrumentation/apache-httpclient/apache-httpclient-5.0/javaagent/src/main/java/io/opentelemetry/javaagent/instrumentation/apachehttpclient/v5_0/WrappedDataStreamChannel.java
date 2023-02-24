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
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.nio.DataStreamChannel;

public final class WrappedDataStreamChannel implements DataStreamChannel {
  private final Context parentContext;
  private final DataStreamChannel delegate;

  public WrappedDataStreamChannel(Context parentContext, DataStreamChannel delegate) {
    this.parentContext = parentContext;
    this.delegate = delegate;
  }

  @Override
  public void requestOutput() {
    delegate.requestOutput();
  }

  @Override
  public int write(ByteBuffer byteBuffer) throws IOException {
    BytesTransferMetrics metrics = createOrGetWithParentContext(parentContext);
    metrics.addRequestBytes(byteBuffer.limit());
    return delegate.write(byteBuffer);
  }

  @Override
  public void endStream() throws IOException {
    delegate.endStream();
  }

  @Override
  public void endStream(List<? extends Header> list) throws IOException {
    delegate.endStream(list);
  }
}
