/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.netty.v4.common;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.implementsInterface;
import static net.bytebuddy.matcher.ElementMatchers.isArray;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.asm.Advice.AssignReturned.ToArguments.ToArgument;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class NettyFutureInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<ClassLoader> classLoaderOptimization() {
    return hasClassesNamed("io.netty.util.concurrent.Future");
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return implementsInterface(named("io.netty.util.concurrent.Future"));
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod()
            .and(named("addListener"))
            .and(takesArgument(0, named("io.netty.util.concurrent.GenericFutureListener"))),
        NettyFutureInstrumentation.class.getName() + "$AddListenerAdvice");
    transformer.applyAdviceToMethod(
        isMethod().and(named("addListeners")).and(takesArgument(0, isArray())),
        NettyFutureInstrumentation.class.getName() + "$AddListenersAdvice");
    transformer.applyAdviceToMethod(
        isMethod()
            .and(named("removeListener"))
            .and(takesArgument(0, named("io.netty.util.concurrent.GenericFutureListener"))),
        NettyFutureInstrumentation.class.getName() + "$RemoveListenerAdvice");
    transformer.applyAdviceToMethod(
        isMethod().and(named("removeListeners")).and(takesArgument(0, isArray())),
        NettyFutureInstrumentation.class.getName() + "$RemoveListenersAdvice");
  }

  @SuppressWarnings("unused")
  public static class AddListenerAdvice {

    @Advice.OnMethodEnter
    @Advice.AssignReturned.ToArguments(@ToArgument(0))
    public static GenericFutureListener<? extends Future<?>> wrapListener(
        @Advice.Argument(value = 0) GenericFutureListener<? extends Future<?>> listenerArg) {

      // TODO remove this extra variable when migrating to "indy only" instrumentation.
      GenericFutureListener<? extends Future<?>> listener = listenerArg;
      if (FutureListenerWrappers.shouldWrap(listener)) {
        listener = FutureListenerWrappers.wrap(Java8BytecodeBridge.currentContext(), listener);
      }
      return listener;
    }
  }

  @SuppressWarnings("unused")
  public static class AddListenersAdvice {

    // here the AsScalar allows to assign the value of the returned array to the argument value,
    // otherwise it's considered to be an Object[] that contains the arguments/return value/thrown
    // exception assignments that bytebuddy has to do after the advice is invoked.
    @Advice.OnMethodEnter
    @Advice.AssignReturned.AsScalar
    @Advice.AssignReturned.ToArguments(@ToArgument(0))
    public static Object[] wrapListener(
        @Advice.Argument(value = 0) GenericFutureListener<? extends Future<?>>[] listeners) {

      Context context = Java8BytecodeBridge.currentContext();
      @SuppressWarnings({"unchecked", "rawtypes"})
      GenericFutureListener<? extends Future<?>>[] wrappedListeners =
          new GenericFutureListener[listeners.length];
      for (int i = 0; i < listeners.length; ++i) {
        if (FutureListenerWrappers.shouldWrap(listeners[i])) {
          wrappedListeners[i] = FutureListenerWrappers.wrap(context, listeners[i]);
        } else {
          wrappedListeners[i] = listeners[i];
        }
      }
      return wrappedListeners;
    }
  }

  @SuppressWarnings("unused")
  public static class RemoveListenerAdvice {

    @Advice.OnMethodEnter
    @Advice.AssignReturned.ToArguments(@ToArgument(0))
    public static GenericFutureListener<? extends Future<?>> wrapListener(
        @Advice.Argument(value = 0) GenericFutureListener<? extends Future<?>> listener) {
      return FutureListenerWrappers.getWrapper(listener);
    }
  }

  @SuppressWarnings("unused")
  public static class RemoveListenersAdvice {

    // here the AsScalar allows to assign the value of the returned array to the argument value,
    // otherwise it's considered to be an Object[] that contains the arguments/return value/thrown
    // exception assignments that bytebuddy has to do after the advice is invoked.
    @Advice.OnMethodEnter
    @Advice.AssignReturned.AsScalar
    @Advice.AssignReturned.ToArguments(@ToArgument(0))
    public static Object[] wrapListener(
        @Advice.Argument(value = 0) GenericFutureListener<? extends Future<?>>[] listeners) {

      @SuppressWarnings({"unchecked", "rawtypes"})
      GenericFutureListener<? extends Future<?>>[] wrappedListeners =
          new GenericFutureListener[listeners.length];
      for (int i = 0; i < listeners.length; ++i) {
        wrappedListeners[i] = FutureListenerWrappers.getWrapper(listeners[i]);
      }
      return wrappedListeners;
    }
  }
}
