/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.play.v2_4;

import io.opentelemetry.instrumentation.api.incubator.semconv.code.CodeAttributesGetter;
import javax.annotation.Nullable;

public class ActionCodeAttributesGetter implements CodeAttributesGetter<ActionData> {
  @Nullable
  @Override
  public Class<?> getCodeClass(ActionData actionData) {
    return actionData.codeClass();
  }

  @Nullable
  @Override
  public String getMethodName(ActionData actionData) {
    return actionData.methodName();
  }
}
