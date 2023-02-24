/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.commons;

import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.util.VirtualField;
import java.util.concurrent.atomic.AtomicLong;

public final class BytesTransferMetrics {
  private static final VirtualField<Context, BytesTransferMetrics> byContext;

  static {
    byContext = VirtualField.find(Context.class, BytesTransferMetrics.class);
  }

  private final AtomicLong bytesOut = new AtomicLong();

  private final AtomicLong bytesIn = new AtomicLong();

  /**
   * Add request bytes produced. This may not always represent the request size, for example in case
   * when connection is closed while writing request, this will repsent the bytes written till this
   * point.
   *
   * @param byteLength bytes written from request.
   */
  public void addRequestBytes(int byteLength) {
    bytesOut.addAndGet(byteLength);
  }

  /**
   * Add response bytes consumed. This may not always represent the response size, for example in
   * case when connection is closed while reading response, this will represent the bytes read till
   * this point.
   *
   * <p>**Note** that this metric may only be applicable for async client, since it fetches response
   * eagerly but, sync client fetches response lazily and hence the bytes may even be consumed after
   * the end of span.
   *
   * @param byteLength bytes consumed from response.
   */
  public void addResponseBytes(int byteLength) {
    bytesIn.addAndGet(byteLength);
  }

  /**
   * Get request content length, priority is given if explicit content-length is present like in
   * case when the request is not chunked, else value is computed using the bytes written.
   *
   * @return content-length of request, null if content-length is not applicable.
   */
  public Long getRequestContentLength() {
    long bytesWritten = bytesOut.get();
    if (bytesWritten > 0) {
      return bytesWritten;
    }
    return null;
  }

  /**
   * Get response content length, priority is given if explicit content-length is present like in
   * case when the response is not chunked, else value is computed using the bytes read.
   *
   * @return content-length of response, null if content-length is not applicable.
   */
  public Long getResponseContentLength() {
    long bytesRead = bytesIn.get();
    if (bytesRead > 0) {
      return bytesRead;
    }
    return null;
  }

  public static BytesTransferMetrics createOrGetWithParentContext(Context parentContext) {
    BytesTransferMetrics metrics = byContext.get(parentContext);
    if (metrics == null) {
      metrics = new BytesTransferMetrics();
      byContext.set(parentContext, metrics);
    }
    return metrics;
  }

  public static BytesTransferMetrics getBytesTransferMetrics(Context parentContext) {
    return byContext.get(parentContext);
  }
}
