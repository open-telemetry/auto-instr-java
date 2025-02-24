/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.camunda.v7_0.behavior;

import io.opentelemetry.context.propagation.TextMapSetter;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;

public class CamundaActivityExecutionLocalSetter implements TextMapSetter<ActivityExecution> {

  @Override
  public void set(ActivityExecution carrier, String key, String value) {
    carrier.setVariableLocal(key, value);
  }
}
