/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.camunda.v7_0.processes;

import io.opentelemetry.context.propagation.TextMapSetter;
import org.camunda.bpm.engine.impl.ProcessInstanceModificationBuilderImpl;

// TODO use this or  activityinstanctiationbuildersetter ??
public class CamundaProcessInstanceModificationBuilderImpSetter
    implements TextMapSetter<ProcessInstanceModificationBuilderImpl> {

  @Override
  public void set(ProcessInstanceModificationBuilderImpl carrier, String key, String value) {
    carrier.setVariable(key, value);
  }
}
