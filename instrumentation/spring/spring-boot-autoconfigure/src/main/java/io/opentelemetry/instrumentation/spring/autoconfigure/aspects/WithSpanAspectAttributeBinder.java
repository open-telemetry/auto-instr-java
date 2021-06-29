/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.spring.autoconfigure.aspects;

import io.opentelemetry.instrumentation.annotation.support.AttributeBindings;
import io.opentelemetry.instrumentation.annotation.support.BaseAttributeBinder;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.core.ParameterNameDiscoverer;

public class WithSpanAspectAttributeBinder extends BaseAttributeBinder {
  private static final ConcurrentMap<Method, AttributeBindings> bindings =
      new ConcurrentHashMap<>();

  private final ParameterNameDiscoverer parameterNameDiscoverer;

  public WithSpanAspectAttributeBinder(ParameterNameDiscoverer parameterNameDiscoverer) {
    this.parameterNameDiscoverer = parameterNameDiscoverer;
  }

  @Override
  public AttributeBindings bind(Method method) {
    return bindings.computeIfAbsent(method, super::bind);
  }

  @Override
  protected @Nullable String[] attributeNamesForParameters(Method method, Parameter[] parameters) {
    String[] parameterNames = parameterNameDiscoverer.getParameterNames(method);
    String[] attributeNames = new String[parameters.length];

    for (int i = 0; i < parameters.length; i++) {
      attributeNames[i] = attributeName(parameters[i], parameterNames, i);
    }
    return attributeNames;
  }

  @Nullable
  private static String attributeName(Parameter parameter, String[] parameterNames, int index) {
    SpanAttribute annotation = parameter.getDeclaredAnnotation(SpanAttribute.class);
    if (annotation == null) {
      return null;
    }
    String value = annotation.value();
    if (!value.isEmpty()) {
      return value;
    }
    if (parameterNames != null && parameterNames.length >= index) {
      String parameterName = parameterNames[index];
      if (parameterName != null && !parameterName.isEmpty()) {
        return parameterName;
      }
    }
    if (parameter.isNamePresent()) {
      return parameter.getName();
    }
    return null;
  }
}
