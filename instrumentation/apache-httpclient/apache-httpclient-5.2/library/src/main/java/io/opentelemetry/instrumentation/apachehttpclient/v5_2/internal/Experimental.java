/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.apachehttpclient.v5_2.internal;

import static java.util.logging.Level.FINE;

import io.opentelemetry.instrumentation.apachehttpclient.v5_2.ApacheHttpClientTelemetryBuilder;
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

  @Nullable private static final Method emitExperimentalTelemetry = getEmitExperimentalTelemetry();

  public void setEmitExperimentalTelemetry(
      ApacheHttpClientTelemetryBuilder builder, boolean emitExperimentalTelemetry) {

    if (Experimental.emitExperimentalTelemetry != null) {
      try {
        Experimental.emitExperimentalTelemetry.invoke(builder, emitExperimentalTelemetry);
      } catch (IllegalAccessException | InvocationTargetException e) {
        logger.log(FINE, e.getMessage(), e);
      }
    }
  }

  @Nullable
  private static Method getEmitExperimentalTelemetry() {
    try {
      Method method =
          ApacheHttpClientTelemetryBuilder.class.getDeclaredMethod(
              "setEmitExperimentalHttpClientMetrics", boolean.class);
      method.setAccessible(true);
      return method;
    } catch (NoSuchMethodException e) {
      logger.log(FINE, e.getMessage(), e);
      return null;
    }
  }
}
