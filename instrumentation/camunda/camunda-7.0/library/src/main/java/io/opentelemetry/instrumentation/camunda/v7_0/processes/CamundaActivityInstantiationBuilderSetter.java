/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.camunda.v7_0.processes;

import io.opentelemetry.context.propagation.TextMapSetter;
import org.camunda.bpm.engine.runtime.ActivityInstantiationBuilder;

// TODO Bound it to ProcessInstanceModificationBuilder ??
public class CamundaActivityInstantiationBuilderSetter
    implements TextMapSetter<ActivityInstantiationBuilder<?>> {

  @Override
  public void set(ActivityInstantiationBuilder<?> carrier, String key, String value) {
    carrier.setVariable(key, value);
  }
}
