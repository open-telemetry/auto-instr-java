/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.camunda.v7_0.behavior;

import io.opentelemetry.context.propagation.TextMapSetter;
import org.camunda.bpm.engine.variable.VariableMap;

public class CamundaVariableMapSetter implements TextMapSetter<VariableMap> {

  @Override
  public void set(VariableMap carrier, String key, String value) {
    carrier.put(key, value);
  }
}
