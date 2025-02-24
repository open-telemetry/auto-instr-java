/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.instrumentationannotations.incubator;

import application.io.opentelemetry.instrumentation.annotations.incubator.MetricAttribute;
import application.io.opentelemetry.instrumentation.annotations.incubator.MetricAttributeForReturnValue;
import application.io.opentelemetry.instrumentation.annotations.incubator.StaticMetricAttribute;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.instrumentation.api.annotation.support.MethodBinder;
import io.opentelemetry.instrumentation.api.annotation.support.ParameterAttributeNamesExtractor;
import io.opentelemetry.semconv.incubating.CodeIncubatingAttributes;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;

public abstract class MetricsAnnotationHelper {
  private static final String INSTRUMENTATION_NAME =
      "io.opentelemetry.opentelemetry-instrumentation-annotations-incubator";
  static final Meter METER = GlobalOpenTelemetry.get().getMeter(INSTRUMENTATION_NAME);
  static final ParameterAttributeNamesExtractor PARAMETER_ATTRIBUTE_NAMES_EXTRACTOR =
      (method, parameters) -> {
        String[] attributeNames = new String[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
          attributeNames[i] = attributeName(parameters[i]);
        }
        return attributeNames;
      };

  static void addStaticAttributes(Method method, AttributesBuilder attributesBuilder) {
    attributesBuilder.put(
        CodeIncubatingAttributes.CODE_NAMESPACE, method.getDeclaringClass().getName());
    attributesBuilder.put(CodeIncubatingAttributes.CODE_FUNCTION_NAME, method.getName());

    StaticMetricAttribute[] staticMetricAttributes =
        method.getDeclaredAnnotationsByType(StaticMetricAttribute.class);
    for (StaticMetricAttribute staticMetricAttribute : staticMetricAttributes) {
      attributesBuilder.put(staticMetricAttribute.name(), staticMetricAttribute.value());
    }
  }

  @Nullable
  private static String attributeName(Parameter parameter) {
    MetricAttribute annotation = parameter.getDeclaredAnnotation(MetricAttribute.class);
    if (annotation == null) {
      return null;
    }
    String value = annotation.value();
    if (!value.isEmpty()) {
      return value;
    } else if (parameter.isNamePresent()) {
      return parameter.getName();
    } else {
      return null;
    }
  }

  static class MetricAttributeHelper {
    private final BiConsumer<AttributesBuilder, Object[]> bindParameters;
    private final BiConsumer<AttributesBuilder, Object> bindReturn;
    private final Attributes staticAttributes;

    MetricAttributeHelper(Method method) {
      bindParameters = MethodBinder.bindParameters(method, PARAMETER_ATTRIBUTE_NAMES_EXTRACTOR);
      MetricAttributeForReturnValue returnValueAttribute =
          method.getAnnotation(MetricAttributeForReturnValue.class);
      bindReturn =
          returnValueAttribute != null
              ? MethodBinder.bindReturnValue(method, returnValueAttribute.value())
              : null;

      AttributesBuilder attributesBuilder = Attributes.builder();
      addStaticAttributes(method, attributesBuilder);
      staticAttributes = attributesBuilder.build();
    }

    Attributes getAttributes(Object returnValue, Object[] arguments, Throwable throwable) {
      AttributesBuilder attributesBuilder = Attributes.builder();
      attributesBuilder.putAll(staticAttributes);
      if (arguments != null && bindParameters != null) {
        bindParameters.accept(attributesBuilder, arguments);
      }
      if (returnValue != null && bindReturn != null) {
        bindReturn.accept(attributesBuilder, returnValue);
      }
      if (throwable != null) {
        attributesBuilder.put("error.type", throwable.getClass().getName());
      }
      return attributesBuilder.build();
    }
  }
}
