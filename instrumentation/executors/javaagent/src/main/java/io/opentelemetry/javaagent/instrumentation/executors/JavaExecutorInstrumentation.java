/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.executors;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasSuperType;
import static io.opentelemetry.javaagent.instrumentation.executors.ExecutorMatchers.executorNameMatcher;
import static io.opentelemetry.javaagent.instrumentation.executors.ExecutorMatchers.isExecutor;
import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.namedOneOf;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.util.VirtualField;
import io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge;
import io.opentelemetry.javaagent.bootstrap.executors.ContextPropagatingRunnable;
import io.opentelemetry.javaagent.bootstrap.executors.ExecutorAdviceHelper;
import io.opentelemetry.javaagent.bootstrap.executors.PropagatedContext;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.Future;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class JavaExecutorInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return executorNameMatcher().and(isExecutor()); // Apply expensive matcher last.
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        named("execute").and(takesArgument(0, Runnable.class)).and(takesArguments(1)),
        JavaExecutorInstrumentation.class.getName() + "$SetExecuteRunnableStateAdvice");
    // Netty uses addTask as the actual core of their submission; there are non-standard variations
    // like execute(Runnable,boolean) that aren't caught by standard instrumentation
    transformer.applyAdviceToMethod(
        named("addTask").and(takesArgument(0, Runnable.class)).and(takesArguments(1)),
        JavaExecutorInstrumentation.class.getName() + "$SetExecuteRunnableStateAdvice");
    transformer.applyAdviceToMethod(
        named("execute").and(takesArgument(0, ForkJoinTask.class)),
        JavaExecutorInstrumentation.class.getName() + "$SetJavaForkJoinStateAdvice");
    transformer.applyAdviceToMethod(
        named("submit")
            .and(takesArgument(0, Runnable.class))
            .and(returns(hasSuperType(is(Future.class)))),
        JavaExecutorInstrumentation.class.getName() + "$SetSubmitRunnableStateAdvice");
    transformer.applyAdviceToMethod(
        named("submit")
            .and(takesArgument(0, Callable.class))
            .and(returns(hasSuperType(is(Future.class)))),
        JavaExecutorInstrumentation.class.getName() + "$SetCallableStateAdvice");
    transformer.applyAdviceToMethod(
        named("submit").and(takesArgument(0, ForkJoinTask.class)),
        JavaExecutorInstrumentation.class.getName() + "$SetJavaForkJoinStateAdvice");
    transformer.applyAdviceToMethod(
        namedOneOf("invokeAny", "invokeAll").and(takesArgument(0, Collection.class)),
        JavaExecutorInstrumentation.class.getName()
            + "$SetCallableStateForCallableCollectionAdvice");
    transformer.applyAdviceToMethod(
        named("invoke").and(takesArgument(0, ForkJoinTask.class)),
        JavaExecutorInstrumentation.class.getName() + "$SetJavaForkJoinStateAdvice");
    transformer.applyAdviceToMethod(
        named("schedule")
            .and(takesArgument(0, Runnable.class))
            .and(returns(hasSuperType(is(Future.class)))),
        JavaExecutorInstrumentation.class.getName() + "$SetSubmitRunnableStateAdvice");
    transformer.applyAdviceToMethod(
        named("schedule")
            .and(takesArgument(0, Callable.class))
            .and(returns(hasSuperType(is(Future.class)))),
        JavaExecutorInstrumentation.class.getName() + "$SetCallableStateAdvice");
  }

  @SuppressWarnings("unused")
  public static class SetExecuteRunnableStateAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static PropagatedContext enterJobSubmit(
        @Advice.This Executor executor,
        @Advice.Argument(value = 0, readOnly = false) Runnable task) {
      Context context = Java8BytecodeBridge.currentContext();
      if (!ExecutorAdviceHelper.shouldPropagateContext(context, task)) {
        return null;
      }
      if (ContextPropagatingRunnable.shouldDecorateRunnable(executor, task)) {
        task = ContextPropagatingRunnable.propagateContext(task, context);
        return null;
      }
      VirtualField<Runnable, PropagatedContext> virtualField =
          VirtualField.find(Runnable.class, PropagatedContext.class);
      return ExecutorAdviceHelper.attachContextToTask(context, virtualField, task);
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void exitJobSubmit(
        @Advice.Enter PropagatedContext propagatedContext, @Advice.Thrown Throwable throwable) {
      ExecutorAdviceHelper.cleanUpAfterSubmit(propagatedContext, throwable);
    }
  }

  @SuppressWarnings("unused")
  public static class SetJavaForkJoinStateAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static PropagatedContext enterJobSubmit(@Advice.Argument(0) ForkJoinTask<?> task) {
      Context context = Java8BytecodeBridge.currentContext();
      if (ExecutorAdviceHelper.shouldPropagateContext(context, task)) {
        VirtualField<ForkJoinTask<?>, PropagatedContext> virtualField =
            VirtualField.find(ForkJoinTask.class, PropagatedContext.class);
        return ExecutorAdviceHelper.attachContextToTask(context, virtualField, task);
      }
      return null;
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void exitJobSubmit(
        @Advice.Enter PropagatedContext propagatedContext, @Advice.Thrown Throwable throwable) {
      ExecutorAdviceHelper.cleanUpAfterSubmit(propagatedContext, throwable);
    }
  }

  @SuppressWarnings("unused")
  public static class SetSubmitRunnableStateAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static PropagatedContext enterJobSubmit(
        @Advice.This Executor executor,
        @Advice.Argument(value = 0, readOnly = false) Runnable task) {
      Context context = Java8BytecodeBridge.currentContext();
      if (!ExecutorAdviceHelper.shouldPropagateContext(context, task)) {
        return null;
      }
      if (ContextPropagatingRunnable.shouldDecorateRunnable(executor, task)) {
        task = ContextPropagatingRunnable.propagateContext(task, context);
        return null;
      }
      VirtualField<Runnable, PropagatedContext> virtualField =
          VirtualField.find(Runnable.class, PropagatedContext.class);
      return ExecutorAdviceHelper.attachContextToTask(context, virtualField, task);
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void exitJobSubmit(
        @Advice.Enter PropagatedContext propagatedContext,
        @Advice.Thrown Throwable throwable,
        @Advice.Return Future<?> future) {
      if (propagatedContext != null && future != null) {
        VirtualField<Future<?>, PropagatedContext> virtualField =
            VirtualField.find(Future.class, PropagatedContext.class);
        virtualField.set(future, propagatedContext);
      }
      ExecutorAdviceHelper.cleanUpAfterSubmit(propagatedContext, throwable);
    }
  }

  @SuppressWarnings("unused")
  public static class SetCallableStateAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static PropagatedContext enterJobSubmit(@Advice.Argument(0) Callable<?> task) {
      Context context = Java8BytecodeBridge.currentContext();
      if (ExecutorAdviceHelper.shouldPropagateContext(context, task)) {
        VirtualField<Callable<?>, PropagatedContext> virtualField =
            VirtualField.find(Callable.class, PropagatedContext.class);
        return ExecutorAdviceHelper.attachContextToTask(context, virtualField, task);
      }
      return null;
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void exitJobSubmit(
        @Advice.Enter PropagatedContext propagatedContext,
        @Advice.Thrown Throwable throwable,
        @Advice.Return Future<?> future) {
      if (propagatedContext != null && future != null) {
        VirtualField<Future<?>, PropagatedContext> virtualField =
            VirtualField.find(Future.class, PropagatedContext.class);
        virtualField.set(future, propagatedContext);
      }
      ExecutorAdviceHelper.cleanUpAfterSubmit(propagatedContext, throwable);
    }
  }

  @SuppressWarnings("unused")
  public static class SetCallableStateForCallableCollectionAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static Collection<?> submitEnter(
        @Advice.Argument(0) Collection<? extends Callable<?>> tasks) {
      if (tasks == null) {
        return Collections.emptyList();
      }

      Context context = Java8BytecodeBridge.currentContext();
      for (Callable<?> task : tasks) {
        if (ExecutorAdviceHelper.shouldPropagateContext(context, task)) {
          VirtualField<Callable<?>, PropagatedContext> virtualField =
              VirtualField.find(Callable.class, PropagatedContext.class);
          ExecutorAdviceHelper.attachContextToTask(context, virtualField, task);
        }
      }

      // returning tasks and not propagatedContexts to avoid allocating another list just for an
      // edge case (exception)
      return tasks;
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void submitExit(
        @Advice.Enter Collection<? extends Callable<?>> tasks, @Advice.Thrown Throwable throwable) {
      /*
       Note1: invokeAny doesn't return any futures so all we need to do for it
       is to make sure we close all scopes in case of an exception.
       Note2: invokeAll does return futures - but according to its documentation
       it actually only returns after all futures have been completed - i.e. it blocks.
       This means we do not need to setup any hooks on these futures, we just need to clear
       any parent spans in case of an error.
       (according to ExecutorService docs and AbstractExecutorService code)
      */
      if (throwable != null) {
        for (Callable<?> task : tasks) {
          if (task != null) {
            VirtualField<Callable<?>, PropagatedContext> virtualField =
                VirtualField.find(Callable.class, PropagatedContext.class);
            PropagatedContext propagatedContext = virtualField.get(task);
            ExecutorAdviceHelper.cleanUpAfterSubmit(propagatedContext, throwable);
          }
        }
      }
    }
  }
}
