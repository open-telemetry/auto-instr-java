/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.thrift;

import static io.opentelemetry.javaagent.instrumentation.thrift.ThriftSingletons.clientInstrumenter;

import io.opentelemetry.context.Context;
import javax.annotation.Nullable;
import org.apache.thrift.async.AsyncMethodCallback;

public final class AsyncMethodCallbackWrapper<T> implements AsyncMethodCallback<T> {
  private final AsyncMethodCallback<T> innerCallback;
  @Nullable public Context context;
  @Nullable public ThriftRequest request;

  public AsyncMethodCallbackWrapper(AsyncMethodCallback<T> inner) {
    innerCallback = inner;
  }

  public void setContext(Context text) {
    context = text;
  }

  @Nullable
  public Context getContext() {
    return this.context;
  }

  @Override
  public void onComplete(T t) {
    innerCallback.onComplete(t);
    if (context != null && request != null) {
      clientInstrumenter().end(context, request, 0, null);
    }
  }

  @Override
  public void onError(Exception e) {
    innerCallback.onError(e);
    if (context != null && request != null) {
      clientInstrumenter().end(context, request, 0, e);
    }
  }
}
