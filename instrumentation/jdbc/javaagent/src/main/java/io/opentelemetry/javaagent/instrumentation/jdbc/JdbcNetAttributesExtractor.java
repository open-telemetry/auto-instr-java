/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.jdbc;

import io.opentelemetry.instrumentation.api.instrumenter.net.NetRequestAttributesExtractor;
import org.checkerframework.checker.nullness.qual.Nullable;

final class JdbcNetAttributesExtractor extends NetRequestAttributesExtractor<DbRequest, Void> {

  @Nullable
  @Override
  protected String transport(DbRequest dbRequest) {
    return null;
  }

  @Nullable
  @Override
  protected String peerName(DbRequest request) {
    return request.getDbInfo().getHost();
  }

  @Nullable
  @Override
  protected Long peerPort(DbRequest request) {
    Integer port = request.getDbInfo().getPort();
    return port == null ? null : port.longValue();
  }

  @Nullable
  @Override
  protected String peerIp(DbRequest request) {
    return null;
  }
}
