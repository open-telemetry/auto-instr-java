/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.testing.exporter;

import io.opentelemetry.exporter.internal.otlp.logs.LogsRequestMarshaler;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.data.LogData;
import io.opentelemetry.sdk.logs.export.LogExporter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class OtlpInMemoryLogExporter implements LogExporter {

  private static final Logger logger = LoggerFactory.getLogger(OtlpInMemoryLogExporter.class);

  private final Queue<byte[]> collectedRequests = new ConcurrentLinkedQueue<>();

  List<byte[]> getCollectedExportRequests() {
    return new ArrayList<>(collectedRequests);
  }

  void reset() {
    collectedRequests.clear();
  }

  @Override
  public CompletableResultCode export(Collection<LogData> logs) {
    for (LogData log : logs) {
      logger.info("Exporting log {}", log);
    }
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try {
      LogsRequestMarshaler.create(logs).writeBinaryTo(bos);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    collectedRequests.add(bos.toByteArray());
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode flush() {
    return CompletableResultCode.ofSuccess();
  }

  @Override
  public CompletableResultCode shutdown() {
    reset();
    return CompletableResultCode.ofSuccess();
  }
}
