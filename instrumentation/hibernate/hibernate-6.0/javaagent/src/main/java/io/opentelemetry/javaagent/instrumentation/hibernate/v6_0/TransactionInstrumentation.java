/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.hibernate.v6_0;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.implementsInterface;
import static io.opentelemetry.javaagent.instrumentation.hibernate.v6_0.Hibernate6Singletons.instrumenter;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.util.VirtualField;
import io.opentelemetry.javaagent.bootstrap.CallDepth;
import io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import io.opentelemetry.javaagent.instrumentation.hibernate.HibernateOperation;
import io.opentelemetry.javaagent.instrumentation.hibernate.HibernateOperationScope;
import io.opentelemetry.javaagent.instrumentation.hibernate.SessionInfo;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.hibernate.Transaction;

public class TransactionInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<ClassLoader> classLoaderOptimization() {
    return hasClassesNamed("org.hibernate.Transaction");
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return implementsInterface(named("org.hibernate.Transaction"));
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod().and(named("commit")).and(takesArguments(0)),
        TransactionInstrumentation.class.getName() + "$TransactionCommitAdvice");
  }

  @SuppressWarnings("unused")
  public static class TransactionCommitAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class, inline = false)
    public static Object startCommit(@Advice.This Transaction transaction) {

      CallDepth callDepth = CallDepth.forClass(HibernateOperation.class);
      if (callDepth.getAndIncrement() > 0) {
        return callDepth;
      }

      VirtualField<Transaction, SessionInfo> transactionVirtualField =
          VirtualField.find(Transaction.class, SessionInfo.class);
      SessionInfo sessionInfo = transactionVirtualField.get(transaction);

      Context parentContext = Java8BytecodeBridge.currentContext();
      HibernateOperation hibernateOperation =
          new HibernateOperation("Transaction.commit", sessionInfo);
      if (!instrumenter().shouldStart(parentContext, hibernateOperation)) {
        return callDepth;
      }

      return HibernateOperationScope.startNew(
          callDepth, hibernateOperation, parentContext, instrumenter());
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void endCommit(
        @Advice.Thrown Throwable throwable, @Advice.Enter Object enterScope) {

      HibernateOperationScope.end(enterScope, instrumenter(), throwable);
    }
  }
}
