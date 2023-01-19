/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.instrumenter.util;

import io.opentelemetry.instrumentation.api.instrumenter.code.CodeAttributesGetter;
import javax.annotation.Nullable;

enum ClassAndMethodAttributesGetter implements CodeAttributesGetter<ClassAndMethod> {
  INSTANCE;

  @Nullable
  @Override
  public Class<?> getCodeClass(ClassAndMethod classAndMethod) {
    return classAndMethod.declaringClass();
  }

  @Nullable
  @Override
  public String getMethodName(ClassAndMethod classAndMethod) {
    return classAndMethod.methodName();
  }
}
