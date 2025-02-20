/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.netty.v4_1;

import static io.opentelemetry.javaagent.instrumentation.netty.v4_1.NettyClientSingletons.clientHandlerFactory;
import static io.opentelemetry.javaagent.instrumentation.netty.v4_1.NettyClientSingletons.sslInstrumenter;
import static io.opentelemetry.javaagent.instrumentation.netty.v4_1.NettyServerSingletons.serverTelemetry;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpRequestEncoder;
import io.netty.handler.codec.http.HttpResponseDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.codec.http.HttpServerCodec;
import io.opentelemetry.instrumentation.api.util.VirtualField;
import io.opentelemetry.instrumentation.netty.common.v4_0.internal.client.NettySslInstrumentationHandler;
import io.opentelemetry.javaagent.bootstrap.CallDepth;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import io.opentelemetry.javaagent.instrumentation.netty.v4.common.AbstractNettyChannelPipelineInstrumentation;
import net.bytebuddy.asm.Advice;

public class NettyChannelPipelineInstrumentation
    extends AbstractNettyChannelPipelineInstrumentation {

  @Override
  public void transform(TypeTransformer transformer) {
    super.transform(transformer);

    transformer.applyAdviceToMethod(
        isMethod()
            .and(nameStartsWith("add").or(named("replace")))
            .and(takesArgument(1, String.class))
            .and(takesArgument(2, named("io.netty.channel.ChannelHandler"))),
        NettyChannelPipelineInstrumentation.class.getName() + "$ChannelPipelineAddAdvice");
  }

  /**
   * When certain handlers are added to the pipeline, we want to add our corresponding tracing
   * handlers. If those handlers are later removed, we also remove our handlers.
   */
  @SuppressWarnings("unused")
  public static class ChannelPipelineAddAdvice {

    @Advice.OnMethodEnter
    public static CallDepth trackCallDepth(@Advice.Argument(2) ChannelHandler handler) {
      // Previously we used one unique call depth tracker for all handlers, using
      // ChannelPipeline.class as a key.
      // The problem with this approach is that it does not work with netty's
      // io.netty.channel.ChannelInitializer which provides an `initChannel` that can be used to
      // `addLast` other handlers. In that case the depth would exceed 0 and handlers added from
      // initializers would not be considered.
      // Using the specific handler key instead of the generic ChannelPipeline.class will help us
      // both to handle such cases and avoid adding our additional handlers in case of internal
      // calls of `addLast` to other method overloads with a compatible signature.
      CallDepth callDepth = CallDepth.forClass(handler.getClass());
      callDepth.getAndIncrement();
      return callDepth;
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void addHandler(
        @Advice.This ChannelPipeline pipeline,
        @Advice.Argument(1) String handlerName,
        @Advice.Argument(2) ChannelHandler handler,
        @Advice.Enter CallDepth callDepth) {

      if (callDepth.decrementAndGet() > 0) {
        return;
      }

      VirtualField<ChannelHandler, ChannelHandler> instrumentationHandlerField =
          VirtualField.find(ChannelHandler.class, ChannelHandler.class);

      // don't add another instrumentation handler if there already is one attached
      if (instrumentationHandlerField.get(handler) != null) {
        return;
      }

      String name = handlerName;
      if (name == null) {
        ChannelHandlerContext context = pipeline.context(handler);
        if (context == null) {
          // probably a ChannelInitializer that was used and removed
          // see the comment above in @Advice.OnMethodEnter
          return;
        }
        name = context.name();
      }

      ChannelHandler ourHandler = null;
      // Server pipeline handlers
      if (handler instanceof HttpServerCodec) {
        ourHandler =
            serverTelemetry()
                .createCombinedHandler(NettyHttpServerResponseBeforeCommitHandler.INSTANCE);
      } else if (handler instanceof HttpRequestDecoder) {
        ourHandler = serverTelemetry().createRequestHandler();
      } else if (handler instanceof HttpResponseEncoder) {
        ourHandler =
            serverTelemetry()
                .createCombinedHandler(NettyHttpServerResponseBeforeCommitHandler.INSTANCE);
        // Client pipeline handlers
      } else if (handler instanceof HttpClientCodec) {
        ourHandler = clientHandlerFactory().createCombinedHandler();
      } else if (handler instanceof HttpRequestEncoder) {
        ourHandler = clientHandlerFactory().createRequestHandler();
      } else if (handler instanceof HttpResponseDecoder) {
        ourHandler = clientHandlerFactory().createResponseHandler();
        // the SslHandler lives in the netty-handler module, using class name comparison to avoid
        // adding a dependency
      } else if (handler.getClass().getName().equals("io.netty.handler.ssl.SslHandler")) {
        ourHandler = new NettySslInstrumentationHandler(sslInstrumenter(), handler);
      }

      if (ourHandler != null) {
        try {
          pipeline.addAfter(name, ourHandler.getClass().getName(), ourHandler);
          // associate our handle with original handler so they could be removed together
          instrumentationHandlerField.set(handler, ourHandler);
        } catch (IllegalArgumentException e) {
          // Prevented adding duplicate handlers.
        }
      }
    }
  }
}
