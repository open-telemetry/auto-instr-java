/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.internal;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.instrumentation.api.instrumenter.ContextSpanProcessor;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class ContextSpanProcessorImpl implements ContextSpanProcessor {
  private static final ContextKey<BiConsumer<Context, Span>> KEY =
      ContextKey.named("opentelemetry-context-span-processor");

  private final BiConsumer<Context, Span> processor;

  public ContextSpanProcessorImpl(BiConsumer<Context, Span> processor) {
    this.processor = processor;
  }

  public static void onStart(Context context, Span span) {
    BiConsumer<Context, Span> processor = fromContextOrNull(context);
    if (processor != null) {
      processor.accept(context, span);
    }
  }

  @Override
  public Context storeInContext(Context context) {
    return storeInContext(context, processor);
  }

  // instrumented by ContextSpanProcessorUtilInstrumentation
  public static Context storeInContext(Context context, BiConsumer<Context, Span> processor) {
    return context.with(KEY, processor);
  }

  @Nullable
  // instrumented by ContextSpanProcessorImplInstrumentation
  public static BiConsumer<Context, Span> fromContextOrNull(Context context) {
    return context.get(KEY);
  }
}
