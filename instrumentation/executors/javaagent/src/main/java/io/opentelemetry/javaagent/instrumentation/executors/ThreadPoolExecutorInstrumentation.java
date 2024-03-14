/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.executors;

import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.none;

import io.opentelemetry.instrumentation.api.util.VirtualField;
import io.opentelemetry.javaagent.bootstrap.executors.ExecutorAdviceHelper;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class ThreadPoolExecutorInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("java.util.concurrent.ThreadPoolExecutor");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isConstructor(), this.getClass().getName() + "$ConstructorAdvice");
    transformer.applyAdviceToMethod(none(), this.getClass().getName() + "$InitAdvice");
  }

  @SuppressWarnings("unused")
  public static class ConstructorAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void exit(
        @Advice.This ThreadPoolExecutor executor,
        @Advice.FieldValue("workQueue") BlockingQueue<?> workQueue) {
      // We can not safely wrap the Runnable if work queue might depend on the actual implementation
      // type of the Runnable. Allow wrapping only when using a BlockingQueue implementation from
      // the jdk.
      if (workQueue.getClass().getClassLoader() != null
          || workQueue instanceof PriorityBlockingQueue) {
        ExecutorAdviceHelper.disableDecorateRunnable(executor);
      }
    }
  }

  @SuppressWarnings("unused")
  public static class InitAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void exit() {
      // ExecutorAdviceHelper is not a helper class so defining the VirtualField there is not enough
      // to get it recognized.
      VirtualField.find(Executor.class, Boolean.class);
    }
  }
}
