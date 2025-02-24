/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.jsonrpc4j.v1_3;

import static io.opentelemetry.javaagent.instrumentation.jsonrpc4j.v1_3.JsonRpcSingletons.SERVER_INVOCATION_LISTENER;
import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;

import com.googlecode.jsonrpc4j.InvocationListener;
import com.googlecode.jsonrpc4j.JsonRpcBasicServer;
import com.googlecode.jsonrpc4j.MultipleInvocationListener;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class JsonRpcServerInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("com.googlecode.jsonrpc4j.JsonRpcBasicServer");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isConstructor(), this.getClass().getName() + "$ConstructorAdvice");

    transformer.applyAdviceToMethod(
        isMethod().and(named("setInvocationListener")),
        this.getClass().getName() + "$SetInvocationListenerAdvice");
  }

  @SuppressWarnings("unused")
  public static class ConstructorAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void setInvocationListener(
        @Advice.This JsonRpcBasicServer jsonRpcServer,
        @Advice.FieldValue(value = "invocationListener", readOnly = false)
            InvocationListener invocationListener) {
      invocationListener = SERVER_INVOCATION_LISTENER;
    }
  }

  @SuppressWarnings("unused")
  public static class SetInvocationListenerAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void setInvocationListener(
        @Advice.This JsonRpcBasicServer jsonRpcServer,
        @Advice.Argument(value = 0, readOnly = false) InvocationListener invocationListener) {
      if (invocationListener == null) {
        invocationListener = SERVER_INVOCATION_LISTENER;
      } else if (invocationListener instanceof MultipleInvocationListener) {
        ((MultipleInvocationListener) invocationListener)
            .addInvocationListener(SERVER_INVOCATION_LISTENER);
      } else if (invocationListener != SERVER_INVOCATION_LISTENER) {
        invocationListener =
            new MultipleInvocationListener(invocationListener, SERVER_INVOCATION_LISTENER);
      }
    }
  }
}
