/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.netty.v4_1;

import io.netty.resolver.AddressResolver;
import io.netty.resolver.AddressResolverGroup;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.Promise;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.netty.common.internal.NettyConnectionRequest;
import io.opentelemetry.instrumentation.netty.common.v4_0.internal.client.NettyConnectionInstrumenter;
import java.net.SocketAddress;
import java.util.List;
import java.util.function.Supplier;

public final class InstrumentedAddressResolverGroup<T extends SocketAddress>
    extends AddressResolverGroup<T> {

  public static <T extends SocketAddress> AddressResolverGroup<T> wrap(
      NettyConnectionInstrumenter instrumenter, AddressResolverGroup<T> delegate) {
    if (delegate == null || delegate instanceof InstrumentedAddressResolverGroup) {
      return delegate;
    }
    return new InstrumentedAddressResolverGroup<>(instrumenter, delegate);
  }

  private final NettyConnectionInstrumenter instrumenter;
  private final AddressResolverGroup<T> delegate;

  private InstrumentedAddressResolverGroup(
      NettyConnectionInstrumenter instrumenter, AddressResolverGroup<T> delegate) {
    this.instrumenter = instrumenter;
    this.delegate = delegate;
  }

  @Override
  public AddressResolver<T> getResolver(EventExecutor executor) {
    return new InstrumentedResolver<>(instrumenter, delegate.getResolver(executor));
  }

  @Override
  public void close() {
    delegate.close();
  }

  @Override
  protected AddressResolver<T> newResolver(EventExecutor eventExecutor) {
    // this method is called from the super class's implementation of `getResolver` which is
    // overridden by this class
    throw new UnsupportedOperationException("This method should never be called");
  }

  private static final class InstrumentedResolver<T extends SocketAddress>
      implements AddressResolver<T> {

    private final NettyConnectionInstrumenter instrumenter;
    private final AddressResolver<T> delegate;

    private InstrumentedResolver(
        NettyConnectionInstrumenter instrumenter, AddressResolver<T> delegate) {
      this.instrumenter = instrumenter;
      this.delegate = delegate;
    }

    @Override
    public boolean isSupported(SocketAddress socketAddress) {
      return delegate.isSupported(socketAddress);
    }

    @Override
    public boolean isResolved(SocketAddress socketAddress) {
      return delegate.isResolved(socketAddress);
    }

    @Override
    public Future<T> resolve(SocketAddress socketAddress) {
      return instrumentResolve(socketAddress, () -> delegate.resolve(socketAddress));
    }

    @Override
    public Future<T> resolve(SocketAddress socketAddress, Promise<T> promise) {
      return instrumentResolve(socketAddress, () -> delegate.resolve(socketAddress, promise));
    }

    @Override
    public Future<List<T>> resolveAll(SocketAddress socketAddress) {
      return instrumentResolve(socketAddress, () -> delegate.resolveAll(socketAddress));
    }

    @Override
    public Future<List<T>> resolveAll(SocketAddress socketAddress, Promise<List<T>> promise) {
      return instrumentResolve(socketAddress, () -> delegate.resolveAll(socketAddress, promise));
    }

    private <U> Future<U> instrumentResolve(
        SocketAddress socketAddress, Supplier<Future<U>> resolveFunc) {
      Context parentContext = Context.current();
      NettyConnectionRequest request = NettyConnectionRequest.resolve(socketAddress);
      if (!instrumenter.shouldStart(parentContext, request)) {
        return resolveFunc.get();
      }
      Context context = instrumenter.start(parentContext, request);
      try {
        Future<U> future = resolveFunc.get();
        return future.addListener(f -> instrumenter.end(context, request, null, f.cause()));
      } catch (Throwable t) {
        instrumenter.end(context, request, null, t);
        throw t;
      }
    }

    @Override
    public void close() {
      delegate.close();
    }
  }
}
