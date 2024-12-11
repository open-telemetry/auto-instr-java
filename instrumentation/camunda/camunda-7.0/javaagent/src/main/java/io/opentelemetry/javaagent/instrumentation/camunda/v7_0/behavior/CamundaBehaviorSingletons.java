/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.camunda.v7_0.behavior;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.camunda.v7_0.behavior.CamundaBehaviorSpanNameExtractor;
import io.opentelemetry.camunda.v7_0.common.CamundaCommonRequest;
import io.opentelemetry.camunda.v7_0.common.CamundaVariableAttributeExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;

public class CamundaBehaviorSingletons {

  private static final Instrumenter<CamundaCommonRequest, String> instrumenter;

  private static final OpenTelemetry opentelemetry;

  private static final boolean propagationEnabled;

  static {
    opentelemetry = GlobalOpenTelemetry.get();

    InstrumenterBuilder<CamundaCommonRequest, String> builder =
        Instrumenter.<CamundaCommonRequest, String>builder(
                opentelemetry,
                "io.opentelemetry.camunda-behavior",
                new CamundaBehaviorSpanNameExtractor())
            .addAttributesExtractor(new CamundaVariableAttributeExtractor());

    instrumenter = builder.buildInstrumenter();
  }

  public static OpenTelemetry getOpentelemetry() {
    return opentelemetry;
  }

  public static Instrumenter<CamundaCommonRequest, String> getInstumenter() {
    return instrumenter;
  }
}
