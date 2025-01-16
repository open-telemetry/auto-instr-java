/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.camunda.v7_0.task;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.camunda.v7_0.common.CamundaCommonRequest;
import io.opentelemetry.instrumentation.camunda.v7_0.common.CamundaVariableAttributeExtractor;
import io.opentelemetry.instrumentation.camunda.v7_0.task.CamundaTaskSpanNameExtractor;

public class CamundaTaskSingletons {

  private static final Instrumenter<CamundaCommonRequest, String> instrumenter;

  private static final OpenTelemetry opentelemetry;

  static {
    opentelemetry = GlobalOpenTelemetry.get();

    InstrumenterBuilder<CamundaCommonRequest, String> builder =
        Instrumenter.<CamundaCommonRequest, String>builder(
                opentelemetry, "io.opentelemetry.camunda-task", new CamundaTaskSpanNameExtractor())
            .addAttributesExtractor(new CamundaVariableAttributeExtractor());

    instrumenter = builder.buildInstrumenter();
  }

  public static OpenTelemetry getOpentelemetry() {
    return opentelemetry;
  }

  public static Instrumenter<CamundaCommonRequest, String> getInstumenter() {
    return instrumenter;
  }

  private CamundaTaskSingletons() {}
}
