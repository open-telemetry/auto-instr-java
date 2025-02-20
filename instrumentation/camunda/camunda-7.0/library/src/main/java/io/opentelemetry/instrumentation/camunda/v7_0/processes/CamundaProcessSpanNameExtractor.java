/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.camunda.v7_0.processes;

import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.opentelemetry.instrumentation.camunda.v7_0.common.CamundaCommonRequest;

public class CamundaProcessSpanNameExtractor implements SpanNameExtractor<CamundaCommonRequest> {

  @Override
  public String extract(CamundaCommonRequest request) {
    return request.getProcessDefinitionKey().get();
  }
}
