/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.armeria.v1_3.internal;

import static java.util.logging.Level.FINE;

import io.opentelemetry.instrumentation.armeria.v1_3.ArmeriaClientTelemetryBuilder;
import io.opentelemetry.instrumentation.armeria.v1_3.ArmeriaServerTelemetryBuilder;
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

  @Nullable private static final Method clientPeerServiceMethod = getClientPeerServiceMethod();

  @Nullable
  private static final Method emitExperimentalHttpServerMetricsMethod =
      getEmitExperimentalHttpServerMetricsMethod();

  public void setEmitExperimentalTelemetry(
      ArmeriaClientTelemetryBuilder builder, boolean emitExperimentalTelemetry) {

    if (emitExperimentalClientTelemetryMethod != null) {
      try {
        emitExperimentalClientTelemetryMethod.invoke(builder, emitExperimentalTelemetry);
      } catch (IllegalAccessException | InvocationTargetException e) {
        logger.log(FINE, e.getMessage(), e);
      }
    }
  }

  public void setEmitExperimentalTelemetry(
      ArmeriaServerTelemetryBuilder builder, boolean emitExperimentalTelemetry) {

    if (emitExperimentalHttpServerMetricsMethod != null) {
      try {
        emitExperimentalHttpServerMetricsMethod.invoke(builder, emitExperimentalTelemetry);
      } catch (IllegalAccessException | InvocationTargetException e) {
        logger.log(FINE, e.getMessage(), e);
      }
    }
  }

  public void setClientPeerService(ArmeriaClientTelemetryBuilder builder, String peerService) {

    if (clientPeerServiceMethod != null) {
      try {
        clientPeerServiceMethod.invoke(builder, peerService);
      } catch (IllegalAccessException | InvocationTargetException e) {
        logger.log(FINE, e.getMessage(), e);
      }
    }
  }

  @Nullable
  private static Method getEmitExperimentalClientTelemetryMethod() {
    try {
      Method method =
          ArmeriaClientTelemetryBuilder.class.getDeclaredMethod(
              "setEmitExperimentalHttpClientMetrics", boolean.class);
      method.setAccessible(true);
      return method;
    } catch (NoSuchMethodException e) {
      logger.log(FINE, e.getMessage(), e);
      return null;
    }
  }

  @Nullable
  private static Method getClientPeerServiceMethod() {
    try {
      Method method =
          ArmeriaClientTelemetryBuilder.class.getDeclaredMethod("setPeerService", String.class);
      method.setAccessible(true);
      return method;
    } catch (NoSuchMethodException e) {
      logger.log(FINE, e.getMessage(), e);
      return null;
    }
  }

  @Nullable
  private static Method getEmitExperimentalHttpServerMetricsMethod() {
    try {
      return ArmeriaServerTelemetryBuilder.class.getMethod(
          "setEmitExperimentalHttpServerMetrics", boolean.class);
    } catch (NoSuchMethodException e) {
      logger.log(FINE, e.getMessage(), e);
      return null;
    }
  }
}
