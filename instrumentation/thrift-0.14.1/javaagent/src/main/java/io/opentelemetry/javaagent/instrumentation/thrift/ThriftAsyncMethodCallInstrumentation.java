/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.thrift;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.extendsClass;
import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.lang.reflect.Field;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.async.TAsyncMethodCall;
import org.apache.thrift.protocol.TProtocol;

public final class ThriftAsyncMethodCallInstrumentation implements TypeInstrumentation {
  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {

    return extendsClass(named("org.apache.thrift.async.TAsyncMethodCall"));
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod().and(named("write_args")).and(takesArguments(1)),
        ThriftAsyncMethodCallInstrumentation.class.getName() + "$MethodAdvice");
    transformer.applyAdviceToMethod(
        isConstructor(),
        ThriftAsyncMethodCallInstrumentation.class.getName() + "$ConstructorAdvice");
  }

  @SuppressWarnings({"unused"})
  public static class MethodAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void methodEnter(
        @Advice.Argument(0) TProtocol protocol, @Advice.This TAsyncMethodCall<Object> methodCall)
        throws IllegalAccessException {
      if (protocol instanceof ClientOutProtocolWrapper) {
        ((ClientOutProtocolWrapper) protocol).request.methodName =
            methodCall.getClass().getSimpleName();
        for (Field f : methodCall.getClass().getDeclaredFields()) {
          f.setAccessible(true);
          Object tmp = f.get(methodCall);
          ((ClientOutProtocolWrapper) protocol)
              .request.addArgs("arg_" + f.getName(), tmp.toString());
        }
      }
    }

    @SuppressWarnings({"unchecked"})
    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void methodExit(
        @Advice.Argument(0) TProtocol protocol, @Advice.This TAsyncMethodCall<?> methodCall)
        throws NoSuchFieldException, IllegalAccessException {
      if (protocol instanceof ClientOutProtocolWrapper) {
        Field field = TAsyncMethodCall.class.getDeclaredField("callback");
        field.setAccessible(true);
        AsyncMethodCallback<Object> callback = (AsyncMethodCallback<Object>) field.get(methodCall);
        if (callback instanceof AsyncMethodCallbackWrapper) {
          ((AsyncMethodCallbackWrapper<?>) callback).context =
              ((ClientOutProtocolWrapper) protocol).context;
          ((AsyncMethodCallbackWrapper<?>) callback).request =
              ((ClientOutProtocolWrapper) protocol).request;
        }
      }
    }
  }

  @SuppressWarnings({"unused", "unchecked"})
  public static class ConstructorAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onExit(@Advice.This TAsyncMethodCall<?> methodCall)
        throws NoSuchFieldException, IllegalAccessException {
      Field field = TAsyncMethodCall.class.getDeclaredField("callback");
      field.setAccessible(true);
      AsyncMethodCallback<Object> callback = (AsyncMethodCallback<Object>) field.get(methodCall);
      if (callback instanceof AsyncMethodCallbackWrapper) {
        return;
      }
      AsyncMethodCallbackWrapper<Object> asyncMethodCallback =
          new AsyncMethodCallbackWrapper<Object>(callback);
      field.set(methodCall, asyncMethodCallback);
    }
  }
}
