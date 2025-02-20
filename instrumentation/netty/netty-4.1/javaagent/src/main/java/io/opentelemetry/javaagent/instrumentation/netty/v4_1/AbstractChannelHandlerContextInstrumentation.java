/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.netty.v4_1;

import static io.opentelemetry.instrumentation.netty.v4_1.internal.client.HttpClientRequestTracingHandler.HTTP_CLIENT_REQUEST;
import static io.opentelemetry.javaagent.instrumentation.netty.v4_1.NettyClientSingletons.instrumenter;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import io.netty.channel.ChannelHandlerContext;
import io.netty.util.Attribute;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.netty.common.internal.NettyErrorHolder;
import io.opentelemetry.instrumentation.netty.common.v4_0.HttpRequestAndChannel;
import io.opentelemetry.instrumentation.netty.v4_1.internal.AttributeKeys;
import io.opentelemetry.instrumentation.netty.v4_1.internal.ServerContext;
import io.opentelemetry.instrumentation.netty.v4_1.internal.ServerContexts;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class AbstractChannelHandlerContextInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("io.netty.channel.AbstractChannelHandlerContext");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod()
            .and(named("invokeExceptionCaught"))
            .and(takesArgument(0, named(Throwable.class.getName()))),
        AbstractChannelHandlerContextInstrumentation.class.getName()
            + "$InvokeExceptionCaughtAdvice");
  }

  @SuppressWarnings("unused")
  public static class InvokeExceptionCaughtAdvice {

    @Advice.OnMethodEnter
    public static void onEnter(
        @Advice.This ChannelHandlerContext ctx, @Advice.Argument(0) Throwable throwable) {

      // we can't rely on exception handling in HttpClientTracingHandler because it can't catch
      // exceptions from handlers that run after it, for example ratpack has ReadTimeoutHandler
      // (trigger ReadTimeoutException) after HttpClientCodec (or handler is inserted after it)
      Attribute<Context> contextAttr = ctx.channel().attr(AttributeKeys.CLIENT_CONTEXT);
      Context clientContext = contextAttr.get();
      if (clientContext != null) {
        ctx.channel().attr(AttributeKeys.CLIENT_PARENT_CONTEXT).remove();
        contextAttr.remove();
        HttpRequestAndChannel request = ctx.channel().attr(HTTP_CLIENT_REQUEST).getAndRemove();
        instrumenter().end(clientContext, request, null, throwable);
        return;
      }
      ServerContext serverContext = ServerContexts.peekFirst(ctx.channel());
      if (serverContext != null) {
        NettyErrorHolder.set(serverContext.context(), throwable);
      }
    }
  }
}
