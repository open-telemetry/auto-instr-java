/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.camunda.v7_0.jobs;

import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.opentelemetry.instrumentation.camunda.v7_0.common.CamundaCommonRequest;

public class CamundaJobSpanNameExtractor implements SpanNameExtractor<CamundaCommonRequest> {

  @Override
  public String extract(CamundaCommonRequest request) {
    return String.format("%s AsyncContinuation", request.getActivityName().get());
  }
}
