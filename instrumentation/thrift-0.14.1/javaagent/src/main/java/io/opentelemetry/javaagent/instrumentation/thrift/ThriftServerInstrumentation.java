/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.thrift;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.extendsClass;
import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;

import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.lang.reflect.Field;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.thrift.protocol.TProtocolFactory;
import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TServerEventHandler;

public final class ThriftServerInstrumentation implements TypeInstrumentation {
  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return extendsClass(named("org.apache.thrift.server.TServer"))
        .and(not(named("org.apache.thrift.server.TServer")));
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod().and(named("getProtocol")),
        ThriftServerInstrumentation.class.getName() + "$ServerAdvice");
    transformer.applyAdviceToMethod(
        isConstructor(), ThriftServerInstrumentation.class.getName() + "$ConstructorAdvice");
  }

  @SuppressWarnings("unused")
  public static class ServerAdvice {
    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void methodExit() {
      return;
    }
  }

  public static class ConstructorAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onExit(
        @Advice.This TServer server,
        @Advice.FieldValue("inputProtocolFactory_") TProtocolFactory inputProtocolFactory)
        throws IllegalAccessException, NoSuchFieldException {

      Field field = TServer.class.getDeclaredField("inputProtocolFactory_");
      field.setAccessible(true);
      ServerProtocolFactoryWrapper factoryWrapper =
          new ServerProtocolFactoryWrapper(inputProtocolFactory);
      field.set(server, factoryWrapper);
    }
  }

  public static class EventHandlerAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onExit(
        @Advice.This TServer server,
        @Advice.FieldValue("eventHandler_") TServerEventHandler eventHandler)
        throws NoSuchFieldException, IllegalAccessException {

      Field eventHandleField = TServer.class.getDeclaredField("eventHandler_");
      eventHandleField.setAccessible(true);
      eventHandleField.set(server, new ThriftServerEventHandler(eventHandler));
    }
  }
}
