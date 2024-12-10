/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.r2dbc.v1_0;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.api.incubator.semconv.net.PeerServiceAttributesExtractor;
import io.opentelemetry.instrumentation.r2dbc.v1_0.internal.shaded.R2dbcTelemetry;
import io.opentelemetry.instrumentation.r2dbc.v1_0.internal.shaded.internal.R2dbcNetAttributesGetter;
import io.opentelemetry.javaagent.bootstrap.internal.AgentCommonConfig;
import io.opentelemetry.javaagent.bootstrap.internal.AgentInstrumentationConfig;

public final class R2dbcSingletons {

  private static final R2dbcTelemetry TELEMETRY =
      R2dbcTelemetry.builder(GlobalOpenTelemetry.get())
          .setStatementSanitizationEnabled(
              AgentInstrumentationConfig.get()
                  .getBoolean(
                      "otel.instrumentation.r2dbc.statement-sanitizer.enabled",
                      AgentCommonConfig.get().isStatementSanitizationEnabled()))
          .addAttributesExtractor(
              PeerServiceAttributesExtractor.create(
                  R2dbcNetAttributesGetter.INSTANCE,
                  AgentCommonConfig.get().getPeerServiceResolver()))
          .build();

  public static R2dbcTelemetry telemetry() {
    return TELEMETRY;
  }

  private R2dbcSingletons() {}
}
