/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.netty.v4_1.internal;

import static java.util.logging.Level.FINE;

import io.opentelemetry.instrumentation.netty.v4_1.NettyClientTelemetryBuilder;
import io.opentelemetry.instrumentation.netty.v4_1.NettyServerTelemetryBuilder;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.logging.Logger;
import javax.annotation.Nullable;

/**
 * This class is internal and experimental. Its APIs are unstable and can change at any time. Its
 * APIs (or a version of them) may be promoted to the public stable API in the future, but no
 * guarantees are made.
 */
public class Experimental {

  private static final Logger logger = Logger.getLogger(Experimental.class.getName());

  @Nullable
  private static final Method emitExperimentalClientTelemetryMethod =
      getEmitExperimentalClientTelemetryMethod();

  @Nullable
  private static final Method emitExperimentalServerTelemetryMethod =
      getEmitExperimentalServerTelemetryMethod();

  public void setEmitExperimentalTelemetry(
      NettyClientTelemetryBuilder builder, boolean emitExperimentalTelemetry) {

    if (emitExperimentalClientTelemetryMethod != null) {
      try {
        emitExperimentalClientTelemetryMethod.invoke(builder, emitExperimentalTelemetry);
      } catch (IllegalAccessException | InvocationTargetException e) {
        logger.log(FINE, e.getMessage(), e);
      }
    }
  }

  public void setEmitExperimentalTelemetry(
      NettyServerTelemetryBuilder builder, boolean emitExperimentalTelemetry) {

    if (emitExperimentalServerTelemetryMethod != null) {
      try {
        emitExperimentalServerTelemetryMethod.invoke(builder, emitExperimentalTelemetry);
      } catch (IllegalAccessException | InvocationTargetException e) {
        logger.log(FINE, e.getMessage(), e);
      }
    }
  }

  @Nullable
  private static Method getEmitExperimentalClientTelemetryMethod() {
    try {
      Method method =
          NettyClientTelemetryBuilder.class.getDeclaredMethod(
              "setEmitExperimentalHttpClientMetrics", boolean.class);
      method.setAccessible(true);
      return method;
    } catch (NoSuchMethodException e) {
      logger.log(FINE, e.getMessage(), e);
      return null;
    }
  }

  @Nullable
  private static Method getEmitExperimentalServerTelemetryMethod() {
    try {
      Method method =
          NettyServerTelemetryBuilder.class.getDeclaredMethod(
              "setEmitExperimentalHttpServerMetrics", boolean.class);
      method.setAccessible(true);
      return method;
    } catch (NoSuchMethodException e) {
      logger.log(FINE, e.getMessage(), e);
      return null;
    }
  }
}
