/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.bootstrap.executors;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.internal.ContextPropagationDebug;
import java.util.concurrent.Executor;

public final class ContextPropagatingRunnable implements Runnable {

  public static boolean shouldDecorateRunnable(Executor executor, Runnable task) {
    return !(task instanceof ContextPropagatingRunnable)
        && ExecutorAdviceHelper.shouldDecorateRunnable(executor);
  }

  public static Runnable propagateContext(Runnable task, Context context) {
    return new ContextPropagatingRunnable(task, context);
  }

  private final Runnable delegate;
  private final Context context;

  private ContextPropagatingRunnable(Runnable delegate, Context context) {
    this.delegate = delegate;
    this.context = ContextPropagationDebug.addDebugInfo(context, delegate);
  }

  @Override
  public void run() {
    try (Scope ignored = context.makeCurrent()) {
      delegate.run();
    }
  }

  public Runnable unwrap() {
    return delegate;
  }
}
