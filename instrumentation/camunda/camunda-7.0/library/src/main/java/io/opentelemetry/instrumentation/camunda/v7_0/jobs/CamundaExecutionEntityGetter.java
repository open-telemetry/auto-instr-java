/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.camunda.v7_0.jobs;

import io.opentelemetry.context.propagation.TextMapGetter;
import org.camunda.bpm.engine.impl.persistence.entity.ExecutionEntity;

public class CamundaExecutionEntityGetter implements TextMapGetter<ExecutionEntity> {

  @Override
  public Iterable<String> keys(ExecutionEntity carrier) {
    return carrier.getVariableNames();
  }

  @Override
  public String get(ExecutionEntity carrier, String key) {
    Object variable = carrier.getVariables().get(key);
    return variable == null ? null : variable.toString();
  }
}
