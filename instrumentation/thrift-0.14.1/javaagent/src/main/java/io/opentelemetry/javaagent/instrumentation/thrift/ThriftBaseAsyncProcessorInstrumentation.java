/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.thrift;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.extendsClass;
import static io.opentelemetry.javaagent.instrumentation.thrift.ThriftSingletons.serverInstrumenter;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.lang.reflect.Field;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.thrift.TBaseAsyncProcessor;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.server.AbstractNonblockingServer;
import org.apache.thrift.transport.TNonblockingSocket;

public final class ThriftBaseAsyncProcessorInstrumentation implements TypeInstrumentation {
  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return extendsClass(named("org.apache.thrift.TBaseAsyncProcessor"));
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod().and(named("process")).and(takesArguments(1)),
        ThriftBaseAsyncProcessorInstrumentation.class.getName() + "$ProcessAdvice");
  }

  public static class ProcessAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void methodEnter(
        @Advice.Argument(0) AbstractNonblockingServer.AsyncFrameBuffer fb,
        @Advice.This TBaseAsyncProcessor<?> processor)
        throws NoSuchFieldException, IllegalAccessException {

      TProtocol inpot = fb.getInputProtocol();
      Field field = AbstractNonblockingServer.FrameBuffer.class.getDeclaredField("trans_");
      field.setAccessible(true);
      TNonblockingSocket trans = (TNonblockingSocket) field.get(fb);
      ((ServerInProtocolWrapper) inpot).request.host =
          trans.getSocketChannel().socket().getInetAddress().getHostAddress();
      ((ServerInProtocolWrapper) inpot).request.port = trans.getSocketChannel().socket().getPort();
      Field field2 = AbstractNonblockingServer.FrameBuffer.class.getDeclaredField("inProt_");
      field2.setAccessible(true);
      field2.set(fb, inpot);
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void methodExit(
        @Advice.Argument(0) AbstractNonblockingServer.AsyncFrameBuffer fb) {
      TProtocol inpot = fb.getInputProtocol();
      if (inpot instanceof ServerInProtocolWrapper) {
        Context context = ((ServerInProtocolWrapper) inpot).context;
        ThriftRequest request = ((ServerInProtocolWrapper) inpot).request;
        if (context != null && request != null) {
          serverInstrumenter().end(context, request, 0, null);
        }
        if (((ServerInProtocolWrapper) inpot).scope != null) {
          ((ServerInProtocolWrapper) inpot).scope.close();
        }
      }
    }
  }
}
