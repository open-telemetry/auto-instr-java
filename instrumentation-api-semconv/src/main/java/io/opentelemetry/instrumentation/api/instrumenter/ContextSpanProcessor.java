/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.instrumenter;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ImplicitContextKeyed;
import io.opentelemetry.instrumentation.api.internal.ContextSpanProcessorImpl;
import java.util.function.BiConsumer;

/**
 * A span processing function that can be stored in context. When stored in context this function
 * will be applied to all spans create while the context is active. Processing function is called
 * synchronously on the execution thread, it should not throw or block the execution thread.
 *
 * <p><strong>NOTE:</strong> This API is <strong>EXPERIMENTAL</strong>, it may be removed or
 * changed.
 */
public interface ContextSpanProcessor extends ImplicitContextKeyed {

  /**
   * Wrap a {@link BiConsumer} so that it can be stored in context as a span processing function.
   */
  static ContextSpanProcessor wrap(BiConsumer<Context, Span> processor) {
    return new ContextSpanProcessorImpl(processor);
  }
}
