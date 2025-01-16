/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.camunda.v7_0.task;

import io.opentelemetry.context.propagation.TextMapGetter;
import org.camunda.bpm.client.task.ExternalTask;

public class CamundaExternalTaskGetter implements TextMapGetter<ExternalTask> {

  @Override
  public Iterable<String> keys(ExternalTask carrier) {
    return carrier.getAllVariables().keySet();
  }

  @Override
  public String get(ExternalTask carrier, String key) {
    Object variable = carrier.getAllVariables().get(key);
    return variable == null ? null : variable.toString();
  }
}
