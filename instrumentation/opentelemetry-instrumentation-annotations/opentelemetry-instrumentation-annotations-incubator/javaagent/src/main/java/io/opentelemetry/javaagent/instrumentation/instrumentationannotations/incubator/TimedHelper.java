/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.instrumentationannotations.incubator;

import static java.util.concurrent.TimeUnit.NANOSECONDS;

import application.io.opentelemetry.instrumentation.annotations.incubator.Timed;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.annotation.support.async.AsyncOperationEndSupport;
import io.opentelemetry.javaagent.instrumentation.instrumentationannotations.MethodRequest;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public final class TimedHelper extends MetricsAnnotationHelper {

  private static final ClassValue<Map<Method, MethodTimer>> timers =
      new ClassValue<Map<Method, MethodTimer>>() {
        @Override
        protected Map<Method, MethodTimer> computeValue(Class<?> type) {
          return new ConcurrentHashMap<>();
        }
      };

  public static Object recordWithAttributes(
      MethodRequest methodRequest, Object returnValue, Throwable throwable, long startNanoTime) {
    return record(
        methodRequest.method(), returnValue, throwable, startNanoTime, methodRequest.args());
  }

  public static Object record(
      Method method, Object returnValue, Throwable throwable, long startNanoTime) {
    return record(method, returnValue, throwable, startNanoTime, null);
  }

  private static Object record(
      Method method,
      Object returnValue,
      Throwable throwable,
      long startNanoTime,
      Object[] arguments) {
    AsyncOperationEndSupport<Method, Object> operationEndSupport =
        AsyncOperationEndSupport.create(
            (context, m, object, error) ->
                getMethodTimer(m).record(object, arguments, error, startNanoTime),
            Object.class,
            method.getReturnType());
    return operationEndSupport.asyncEnd(Context.current(), method, returnValue, throwable);
  }

  private static MethodTimer getMethodTimer(Method method) {
    return timers.get(method.getDeclaringClass()).computeIfAbsent(method, MethodTimer::new);
  }

  private static String timeUnitToString(TimeUnit timeUnit) {
    switch (timeUnit) {
      case NANOSECONDS:
        return "ns";
      case MICROSECONDS:
        return "us";
      case MILLISECONDS:
        return "ms";
      case SECONDS:
        return "s";
      case MINUTES:
        return "min";
      case HOURS:
        return "h";
      case DAYS:
        return "d";
    }
    throw new IllegalArgumentException("Unsupported time unit " + timeUnit);
  }

  private static double getDuration(long startNanoTime, TimeUnit unit) {
    long nanoDelta = System.nanoTime() - startNanoTime;
    return (double) nanoDelta / NANOSECONDS.convert(1, unit);
  }

  private static class MethodTimer {
    private final TimeUnit unit;
    private final DoubleHistogram histogram;
    private final MetricAttributeHelper attributeHelper;

    MethodTimer(Method method) {
      Timed timedAnnotation = method.getAnnotation(Timed.class);
      unit = timedAnnotation.unit();
      histogram =
          METER
              .histogramBuilder(timedAnnotation.value())
              .setDescription(timedAnnotation.description())
              .setUnit(timeUnitToString(unit))
              .build();
      attributeHelper = new MetricAttributeHelper(method);
    }

    void record(Object returnValue, Object[] arguments, Throwable throwable, long startNanoTime) {
      double duration = getDuration(startNanoTime, unit);
      histogram.record(duration, attributeHelper.getAttributes(returnValue, arguments, throwable));
    }
  }

  private TimedHelper() {}
}
