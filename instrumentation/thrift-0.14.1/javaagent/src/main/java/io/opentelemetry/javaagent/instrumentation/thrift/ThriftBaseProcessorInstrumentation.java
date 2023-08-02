/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.thrift;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.extendsClass;
import static io.opentelemetry.javaagent.instrumentation.thrift.ThriftSingletons.serverInstrumenter;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;

import io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.thrift.ProcessFunction;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TNonblockingSocket;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TTransport;

public final class ThriftBaseProcessorInstrumentation implements TypeInstrumentation {
  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return extendsClass(named("org.apache.thrift.ProcessFunction"));
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod().and(named("process")),
        ThriftBaseProcessorInstrumentation.class.getName() + "$ProcessAdvice");
  }

  public static class ProcessAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void methodEnter(
        @Advice.Argument(1) TProtocol inpot, @Advice.This ProcessFunction<?, ?> processFunction) {
      if (inpot instanceof ServerInProtocolWrapper) {
        TTransport transport = ((ServerInProtocolWrapper) inpot).getRawTransport();

        if (transport instanceof TSocket) {
          ((ServerInProtocolWrapper) inpot).request.host =
              ((TSocket) transport).getSocket().getInetAddress().toString();
          ((ServerInProtocolWrapper) inpot).request.port =
              ((TSocket) transport).getSocket().getPort();
        } else if (transport instanceof TNonblockingSocket) {
          ((ServerInProtocolWrapper) inpot).request.host =
              ((TNonblockingSocket) transport)
                  .getSocketChannel()
                  .socket()
                  .getInetAddress()
                  .getHostAddress();
          ((ServerInProtocolWrapper) inpot).request.port =
              ((TNonblockingSocket) transport).getSocketChannel().socket().getPort();
        }

        ((ServerInProtocolWrapper) inpot).request.methodName = processFunction.getMethodName();
      }
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void methodExit(
        @Advice.Argument(1) TProtocol inpot, @Advice.This ProcessFunction<?, ?> processFunction) {
      if (inpot instanceof ServerInProtocolWrapper) {
        ((ServerInProtocolWrapper) inpot).request.methodName = processFunction.getMethodName();
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
