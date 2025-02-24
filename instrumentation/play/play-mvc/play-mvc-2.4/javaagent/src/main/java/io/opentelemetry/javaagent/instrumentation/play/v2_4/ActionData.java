/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.play.v2_4;

import java.lang.reflect.Method;

public class ActionData {
  private final Class<?> target;
  private final Method method;

  public ActionData(Class<?> target, Method method) {
    this.target = target;
    this.method = method;
  }

  public Class<?> codeClass() {
    return target;
  }

  public String methodName() {
    return method.getName();
  }
}
