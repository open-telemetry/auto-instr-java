/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.junit.v5_0;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor;
import org.junit.jupiter.api.extension.ExtensionContext;

class JunitSingletons {

  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.junit-5.0";
  private static final Instrumenter<ExtensionContext, Object> INSTRUMENTER;

  static {
    InstrumenterBuilder<ExtensionContext, Object> builder =
        Instrumenter.builder(
                GlobalOpenTelemetry.get(), INSTRUMENTATION_NAME, new JunitSpanNameExtractor())
            .addAttributesExtractor(JunitAttributesExtractor.create());
    INSTRUMENTER = builder.buildInstrumenter(SpanKindExtractor.alwaysInternal());
  }

  public static Instrumenter<ExtensionContext, Object> instrumenter() {
    return INSTRUMENTER;
  }

  private JunitSingletons() {}
}
