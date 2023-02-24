/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.commons;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CountingOutputStream extends FilterOutputStream {
  private final BytesTransferMetrics metrics;
  private final AtomicBoolean closed;

  /**
   * Wraps another output stream, counting the number of bytes written.
   *
   * @param out the output stream to be wrapped
   */
  public CountingOutputStream(BytesTransferMetrics metrics, OutputStream out) {
    super(out);
    this.metrics = metrics;
    this.closed = new AtomicBoolean(false);
  }

  @Override
  public void write(byte[] b, int off, int len) throws IOException {
    out.write(b, off, len);
    if (!closed.get()) {
      metrics.addRequestBytes(len);
    }
  }

  @Override
  public void write(int b) throws IOException {
    out.write(b);
    if (!closed.get()) {
      metrics.addRequestBytes(1);
    }
  }

  // Overriding close() because FilterOutputStream's close() method pre-JDK8 has bad behavior:
  // it silently ignores any exception thrown by flush(). Instead, just close the delegate stream.
  // It should flush itself if necessary.
  @Override
  public void close() throws IOException {
    out.close();
    closed.compareAndSet(false, true);
  }
}
