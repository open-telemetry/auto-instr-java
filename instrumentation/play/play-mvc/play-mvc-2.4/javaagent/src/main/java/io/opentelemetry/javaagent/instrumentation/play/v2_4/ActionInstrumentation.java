/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.play.v2_4;

import static io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge.currentContext;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.implementsInterface;
import static io.opentelemetry.javaagent.instrumentation.play.v2_4.Play24Singletons.updateSpan;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.lang.reflect.Method;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import play.api.mvc.Action;
import play.api.mvc.Request;
import play.api.mvc.Result;
import scala.concurrent.Future;

public class ActionInstrumentation implements TypeInstrumentation {
  @Override
  public ElementMatcher<ClassLoader> classLoaderOptimization() {
    return hasClassesNamed("play.api.mvc.Action");
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return implementsInterface(named("play.api.mvc.Action"));
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        named("apply")
            .and(takesArgument(0, named("play.api.mvc.Request")))
            .and(returns(named("scala.concurrent.Future"))),
        this.getClass().getName() + "$ApplyAdvice");
  }

  @SuppressWarnings("unused")
  public static class ApplyAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static ActionScope onEnter(
        @Advice.This Object target,
        @Advice.Origin Method method,
        @Advice.Argument(0) Request<?> req) {
      Context parentContext = currentContext();

      ActionData actionData = new ActionData(target.getClass(), method);
      return ActionScope.start(parentContext, actionData);
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void stopTraceOnResponse(
        @Advice.This Object thisAction,
        @Advice.Thrown Throwable throwable,
        @Advice.Argument(0) Request<?> req,
        @Advice.Return(readOnly = false) Future<Result> responseFuture,
        @Advice.Enter ActionScope actionScope) {
      if (actionScope == null || actionScope.getScope() == null) {
        return;
      }
      actionScope.closeScope();

      updateSpan(actionScope.getContext(), req);

      // span finished in RequestCompleteCallback
      if (throwable == null) {
        responseFuture.onComplete(
            new RequestCompleteCallback(actionScope.getContext()),
            ((Action<?>) thisAction).executionContext());
      } else {
        actionScope.end(throwable);
      }
    }
  }
}
