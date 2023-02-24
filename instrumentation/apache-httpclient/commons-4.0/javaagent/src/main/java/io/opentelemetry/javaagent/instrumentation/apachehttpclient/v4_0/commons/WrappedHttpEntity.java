/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.v4_0.commons;

import io.opentelemetry.javaagent.instrumentation.apachehttpclient.commons.BytesTransferMetrics;
import io.opentelemetry.javaagent.instrumentation.apachehttpclient.commons.CountingOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import org.apache.http.HttpEntity;
import org.apache.http.entity.HttpEntityWrapper;

public final class WrappedHttpEntity extends HttpEntityWrapper {
  private final BytesTransferMetrics metrics;

  public WrappedHttpEntity(BytesTransferMetrics metrics, HttpEntity wrappedEntity) {
    super(wrappedEntity);
    this.metrics = metrics;
  }

  @Override
  public void writeTo(OutputStream outStream) throws IOException {
    super.writeTo(new CountingOutputStream(metrics, outStream));
  }
}
