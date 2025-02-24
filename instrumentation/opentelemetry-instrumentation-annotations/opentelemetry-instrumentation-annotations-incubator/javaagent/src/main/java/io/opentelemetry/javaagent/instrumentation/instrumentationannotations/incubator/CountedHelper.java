/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.instrumentationannotations.incubator;

import application.io.opentelemetry.instrumentation.annotations.incubator.Counted;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.annotation.support.async.AsyncOperationEndSupport;
import io.opentelemetry.javaagent.instrumentation.instrumentationannotations.MethodRequest;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class CountedHelper extends MetricsAnnotationHelper {

  private static final ClassValue<Map<Method, MethodCounter>> counters =
      new ClassValue<Map<Method, MethodCounter>>() {
        @Override
        protected Map<Method, MethodCounter> computeValue(Class<?> type) {
          return new ConcurrentHashMap<>();
        }
      };

  public static Object recordWithAttributes(
      MethodRequest methodRequest, Object returnValue, Throwable throwable) {
    return record(methodRequest.method(), returnValue, throwable, methodRequest.args());
  }

  public static Object record(Method method, Object returnValue, Throwable throwable) {
    return record(method, returnValue, throwable, null);
  }

  private static Object record(
      Method method, Object returnValue, Throwable throwable, Object[] arguments) {
    AsyncOperationEndSupport<Method, Object> operationEndSupport =
        AsyncOperationEndSupport.create(
            (context, m, object, error) -> getMethodCounter(m).record(object, arguments, error),
            Object.class,
            method.getReturnType());
    return operationEndSupport.asyncEnd(Context.current(), method, returnValue, throwable);
  }

  private static MethodCounter getMethodCounter(Method method) {
    return counters.get(method.getDeclaringClass()).computeIfAbsent(method, MethodCounter::new);
  }

  private static class MethodCounter {
    private final LongCounter counter;
    private final MetricAttributeHelper attributeHelper;

    MethodCounter(Method method) {
      Counted countedAnnotation = method.getAnnotation(Counted.class);
      counter =
          METER
              .counterBuilder(countedAnnotation.value())
              .setDescription(countedAnnotation.description())
              .setUnit(countedAnnotation.unit())
              .build();
      attributeHelper = new MetricAttributeHelper(method);
    }

    void record(Object returnValue, Object[] arguments, Throwable throwable) {
      counter.add(1, attributeHelper.getAttributes(returnValue, arguments, throwable));
    }
  }

  private CountedHelper() {}
}
