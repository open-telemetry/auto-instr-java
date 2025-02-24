/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.netty.common.v4_0.internal.client;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.util.VirtualField;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.SocketAddress;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
// inspired by reactor-netty SslProvider.SslReadHandler
public final class NettySslInstrumentationHandler extends ChannelDuplexHandler {

  private static final Class<?> SSL_HANDSHAKE_COMPLETION_EVENT;
  private static final MethodHandle GET_CAUSE;

  // this is used elsewhere to manage the link between the underlying (user) handler and our handler
  // which is needed below so that we can unlink this handler when we remove it below
  private static final VirtualField<ChannelHandler, ChannelHandler> instrumentationHandlerField =
      VirtualField.find(ChannelHandler.class, ChannelHandler.class);

  static {
    Class<?> sslHandshakeCompletionEvent = null;
    MethodHandle getCause = null;
    try {
      sslHandshakeCompletionEvent =
          Class.forName(
              "io.netty.handler.ssl.SslHandshakeCompletionEvent",
              false,
              NettySslInstrumentationHandler.class.getClassLoader());
      getCause =
          MethodHandles.lookup()
              .findVirtual(
                  sslHandshakeCompletionEvent, "cause", MethodType.methodType(Throwable.class));
    } catch (Throwable t) {
      // no SSL classes on classpath
    }
    SSL_HANDSHAKE_COMPLETION_EVENT = sslHandshakeCompletionEvent;
    GET_CAUSE = getCause;
  }

  private final NettySslInstrumenter instrumenter;
  private final ChannelHandler realHandler;
  private Context parentContext;
  private NettySslRequest request;
  private Context context;

  public NettySslInstrumentationHandler(
      NettySslInstrumenter instrumenter, ChannelHandler realHandler) {
    this.instrumenter = instrumenter;
    this.realHandler = realHandler;
  }

  @Override
  public void channelRegistered(ChannelHandlerContext ctx) throws Exception {
    // this should never happen at this point (since the handler is only registered when SSL classes
    // are on classpath); checking just to be extra safe
    if (SSL_HANDSHAKE_COMPLETION_EVENT == null) {
      ctx.pipeline().remove(this);
      instrumentationHandlerField.set(realHandler, null);
      super.channelRegistered(ctx);
      return;
    }

    // remember the parent context from the time of channel registration;
    // this happens inside Bootstrap#connect()
    parentContext = Context.current();
    super.channelRegistered(ctx);
  }

  @Override
  public void connect(
      ChannelHandlerContext ctx,
      SocketAddress remoteAddress,
      SocketAddress localAddress,
      ChannelPromise promise)
      throws Exception {

    // netty SslHandler starts the handshake after it receives the channelActive() signal; this
    // happens just after the connection is established
    // this makes connect() promise a good place to start the SSL handshake span
    promise.addListener(
        future -> {
          // there won't be any SSL handshake if the channel fails to connect
          // give up when channelRegistered wasn't called and parentContext is null
          if (!future.isSuccess() || parentContext == null) {
            return;
          }
          request = NettySslRequest.create(ctx.channel());
          if (instrumenter.shouldStart(parentContext, request)) {
            context = instrumenter.start(parentContext, request);
          }
        });
    super.connect(ctx, remoteAddress, localAddress, promise);
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    if (SSL_HANDSHAKE_COMPLETION_EVENT.isInstance(evt)) {
      if (ctx.pipeline().context(this) != null) {
        ctx.pipeline().remove(this);
        instrumentationHandlerField.set(realHandler, null);
      }

      if (context != null) {
        instrumenter.end(context, request, getCause(evt));
      }
    }

    super.userEventTriggered(ctx, evt);
  }

  private static Throwable getCause(Object evt) {
    try {
      return (Throwable) GET_CAUSE.invoke(evt);
    } catch (Throwable e) {
      // should not ever happen
      return null;
    }
  }
}
