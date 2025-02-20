/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.netty.v4.common;

import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;
import io.netty.util.concurrent.GenericProgressiveFutureListener;
import io.netty.util.concurrent.ProgressiveFuture;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.util.VirtualField;
import java.lang.ref.WeakReference;

public final class FutureListenerWrappers {
  // note: it's ok if the value is collected prior to the key, since this cache is only used to
  // remove the wrapped listener from the netty future, and if the value is collected prior to the
  // key, that means it's no longer used (referenced) by the netty future anyways.
  //
  // also note: this is using WeakReference as VirtualField value in case this is ever converted to
  // library instrumentation, because while the library implementation of VirtualField maintains a
  // weak reference to its keys, it maintains a strong reference to its values, and in this
  // particular case the wrapper listener (value) has a strong reference to original listener (key),
  // which will create a memory leak. which is not a problem in the javaagent's implementation of
  // VirtualField, since it injects the value directly into the key as a field, and so the value is
  // only retained strongly by the key, and so they can be collected together.
  private static final VirtualField<
          GenericFutureListener<? extends Future<?>>,
          WeakReference<GenericFutureListener<? extends Future<?>>>>
      wrapperVirtualField = VirtualField.find(GenericFutureListener.class, WeakReference.class);

  private static final ClassValue<Boolean> shouldWrap =
      new ClassValue<Boolean>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
          // we only want to wrap user callbacks
          String className = type.getName();
          return !className.startsWith("io.opentelemetry.javaagent.")
              && !className.startsWith("io.netty.");
        }
      };

  public static boolean shouldWrap(GenericFutureListener<? extends Future<?>> listener) {
    return listener != null && shouldWrap.get(listener.getClass());
  }

  @SuppressWarnings("unchecked")
  public static GenericFutureListener<?> wrap(
      Context context, GenericFutureListener<? extends Future<?>> delegate) {

    // note: not using computeIfAbsent because that leaves window where WeakReference can be
    // collected before we have a chance to make (and return) a strong reference to the wrapper

    WeakReference<GenericFutureListener<? extends Future<?>>> resultReference =
        wrapperVirtualField.get(delegate);

    if (resultReference != null) {
      GenericFutureListener<? extends Future<?>> wrapper = resultReference.get();
      if (wrapper != null) {
        return wrapper;
      }
      // note that it's ok if the value is collected prior to the key, since this cache is only
      // used to remove the wrapped listener from the netty future, and if the value is collected
      // prior
      // to the key, that means it's no longer used (referenced) by the netty future anyways.
    }

    GenericFutureListener<? extends Future<?>> wrapper;
    if (delegate instanceof GenericProgressiveFutureListener) {
      wrapper =
          new WrappedProgressiveFutureListener(
              context, (GenericProgressiveFutureListener<ProgressiveFuture<?>>) delegate);
    } else {
      wrapper = new WrappedFutureListener(context, (GenericFutureListener<Future<?>>) delegate);
    }
    wrapperVirtualField.set(delegate, new WeakReference<>(wrapper));
    return wrapper;
  }

  public static GenericFutureListener<? extends Future<?>> getWrapper(
      GenericFutureListener<? extends Future<?>> delegate) {
    WeakReference<GenericFutureListener<? extends Future<?>>> wrapperReference =
        wrapperVirtualField.get(delegate);
    if (wrapperReference == null) {
      return delegate;
    }
    GenericFutureListener<? extends Future<?>> wrapper = wrapperReference.get();
    return wrapper == null ? delegate : wrapper;
  }

  private static final class WrappedFutureListener implements GenericFutureListener<Future<?>> {

    private final Context context;
    private final GenericFutureListener<Future<?>> delegate;

    private WrappedFutureListener(Context context, GenericFutureListener<Future<?>> delegate) {
      this.context = context;
      this.delegate = delegate;
    }

    @Override
    public void operationComplete(Future<?> future) throws Exception {
      try (Scope ignored = context.makeCurrent()) {
        delegate.operationComplete(future);
      }
    }
  }

  private static final class WrappedProgressiveFutureListener
      implements GenericProgressiveFutureListener<ProgressiveFuture<?>> {

    private final Context context;
    private final GenericProgressiveFutureListener<ProgressiveFuture<?>> delegate;

    private WrappedProgressiveFutureListener(
        Context context, GenericProgressiveFutureListener<ProgressiveFuture<?>> delegate) {
      this.context = context;
      this.delegate = delegate;
    }

    @Override
    public void operationProgressed(ProgressiveFuture<?> progressiveFuture, long l, long l1)
        throws Exception {
      try (Scope ignored = context.makeCurrent()) {
        delegate.operationProgressed(progressiveFuture, l, l1);
      }
    }

    @Override
    public void operationComplete(ProgressiveFuture<?> progressiveFuture) throws Exception {
      try (Scope ignored = context.makeCurrent()) {
        delegate.operationComplete(progressiveFuture);
      }
    }
  }

  private FutureListenerWrappers() {}
}
