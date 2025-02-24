/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.internal;

import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Helper class for sharing computed values between different {@link AttributesExtractor}s and
 * {@link SpanNameExtractor} called in the start phase of the {@link Instrumenter}.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public final class InstrumenterContext {
  private static final ThreadLocal<InstrumenterContext> instrumenterContext = new ThreadLocal<>();

  private final Map<String, Object> map = new HashMap<>();
  private int useCount;

  private InstrumenterContext() {}

  @SuppressWarnings("unchecked")
  public static <T> T computeIfAbsent(String key, Function<String, T> function) {
    InstrumenterContext context = instrumenterContext.get();
    if (context == null) {
      return function.apply(key);
    }
    return (T) context.map.computeIfAbsent(key, function);
  }

  // visible for testing
  static Map<String, Object> get() {
    return instrumenterContext.get().map;
  }

  public static boolean isActive() {
    return instrumenterContext.get() != null;
  }

  public static <T> T withContext(Supplier<T> action) {
    InstrumenterContext context = instrumenterContext.get();
    if (context == null) {
      context = new InstrumenterContext();
      instrumenterContext.set(context);
    }
    context.useCount++;
    try {
      return action.get();
    } finally {
      context.useCount--;
      if (context.useCount == 0) {
        instrumenterContext.remove();
      }
    }
  }
}
