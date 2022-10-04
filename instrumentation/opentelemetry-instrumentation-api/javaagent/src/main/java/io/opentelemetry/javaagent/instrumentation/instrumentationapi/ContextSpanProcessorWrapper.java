/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.instrumentationapi;

import application.io.opentelemetry.api.trace.Span;
import application.io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.instrumentation.opentelemetryapi.context.AgentContextStorage;
import io.opentelemetry.javaagent.instrumentation.opentelemetryapi.trace.Bridging;
import java.util.function.BiConsumer;

/**
 * Wrapper that translates agent context and span to application context and span before invoking
 * the delegate.
 */
public final class ContextSpanProcessorWrapper
    implements BiConsumer<io.opentelemetry.context.Context, io.opentelemetry.api.trace.Span> {

  private final BiConsumer<Context, Span> delegate;

  private ContextSpanProcessorWrapper(BiConsumer<Context, Span> delegate) {
    this.delegate = delegate;
  }

  public static BiConsumer<io.opentelemetry.context.Context, io.opentelemetry.api.trace.Span> wrap(
      BiConsumer<Context, Span> processor) {
    return new ContextSpanProcessorWrapper(processor);
  }

  public static BiConsumer<Context, Span> unwrap(
      BiConsumer<io.opentelemetry.context.Context, io.opentelemetry.api.trace.Span> processor) {
    if (processor instanceof ContextSpanProcessorWrapper) {
      return ((ContextSpanProcessorWrapper) processor).delegate;
    }
    return null;
  }

  @Override
  public void accept(
      io.opentelemetry.context.Context context, io.opentelemetry.api.trace.Span span) {
    delegate.accept(
        AgentContextStorage.toApplicationContext(context), Bridging.toApplication(span));
  }
}
