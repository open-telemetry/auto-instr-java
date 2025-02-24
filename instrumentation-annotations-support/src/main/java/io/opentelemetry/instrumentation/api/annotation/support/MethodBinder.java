/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.annotation.support;

import io.opentelemetry.api.common.AttributesBuilder;
import java.lang.reflect.Method;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;

/** Helper class for binding method parameters and return value to attributes. */
public final class MethodBinder {

  /** Create binding for method return value. */
  @Nullable
  public static BiConsumer<AttributesBuilder, Object> bindReturnValue(
      Method method, String attributeName) {
    Class<?> returnType = method.getReturnType();
    if (returnType == void.class) {
      return null;
    }
    AttributeBinding binding = AttributeBindingFactory.createBinding(attributeName, returnType);
    return binding::apply;
  }

  /** Create binding for method parameters. */
  @Nullable
  public static BiConsumer<AttributesBuilder, Object[]> bindParameters(
      Method method, ParameterAttributeNamesExtractor parameterAttributeNamesExtractor) {
    AttributeBindings bindings = AttributeBindings.bind(method, parameterAttributeNamesExtractor);
    if (bindings.isEmpty()) {
      return null;
    }
    return bindings::apply;
  }

  private MethodBinder() {}
}
