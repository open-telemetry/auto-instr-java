/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.instrumentationannotations;

import io.opentelemetry.instrumentation.api.incubator.semconv.code.CodeAttributesGetter;
import java.lang.reflect.Method;

enum MethodCodeAttributesGetter implements CodeAttributesGetter<Method> {
  INSTANCE;

  @Override
  public Class<?> getCodeClass(Method method) {
    return method.getDeclaringClass();
  }

  @Override
  public String getMethodName(Method method) {
    return method.getName();
  }
}
