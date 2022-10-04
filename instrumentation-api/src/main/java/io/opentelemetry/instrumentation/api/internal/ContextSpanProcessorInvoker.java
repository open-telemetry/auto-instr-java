/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.internal;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import java.lang.reflect.Method;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class ContextSpanProcessorInvoker {
  private static final Method onStartMethod = getMethod();

  private static Method getMethod() {
    try {
      Class<?> clazz =
          Class.forName("io.opentelemetry.instrumentation.api.internal.ContextSpanProcessorImpl");
      return clazz.getMethod("onStart", Context.class, Span.class);
    } catch (ClassNotFoundException | NoSuchMethodException exception) {
      return null;
    }
  }

  public static void onStart(Context context, Span span) {
    if (onStartMethod == null) {
      return;
    }
    try {
      onStartMethod.invoke(null, context, span);
    } catch (Exception exception) {
      throw new IllegalStateException(exception);
    }
  }

  private ContextSpanProcessorInvoker() {}
}
