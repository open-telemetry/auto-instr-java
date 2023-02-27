/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.cassandra.v4_0;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.cassandra.v4_4.CassandraTelemetry;
import io.opentelemetry.javaagent.bootstrap.internal.CommonConfig;

final class CassandraSingletons {
  static final CassandraTelemetry telemetry =
      CassandraTelemetry.builder(GlobalOpenTelemetry.get())
          .setStatementSanitizationEnabled(CommonConfig.get().isStatementSanitizationEnabled())
          .build();

  private CassandraSingletons() {}
}
