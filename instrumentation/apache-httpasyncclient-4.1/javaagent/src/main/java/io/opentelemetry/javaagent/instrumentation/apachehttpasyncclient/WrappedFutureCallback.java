/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpasyncclient;

import static io.opentelemetry.javaagent.instrumentation.apachehttpasyncclient.ApacheHttpAsyncClientSingletons.helper;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.javaagent.instrumentation.apachehttpclient.v4_0.commons.ApacheHttpClientRequest;
import java.util.logging.Logger;
import org.apache.http.concurrent.FutureCallback;

public final class WrappedFutureCallback<T> implements FutureCallback<T> {
  private static final Logger logger = Logger.getLogger(WrappedFutureCallback.class.getName());

  private final Context parentContext;
  private final FutureCallback<T> delegate;

  volatile Context context;
  volatile ApacheHttpClientRequest otelRequest;

  public WrappedFutureCallback(Context parentContext, FutureCallback<T> delegate) {
    this.parentContext = parentContext;
    // Note: this can be null in real life, so we have to handle this carefully
    this.delegate = delegate;
  }

  @Override
  public void completed(T result) {
    if (context == null) {
      // this is unexpected
      logger.fine("context was never set");
      completeDelegate(result);
      return;
    }

    helper().endInstrumentation(context, otelRequest, result, null);

    if (parentContext == null) {
      completeDelegate(result);
      return;
    }

    try (Scope ignored = parentContext.makeCurrent()) {
      completeDelegate(result);
    }
  }

  @Override
  public void failed(Exception ex) {
    if (context == null) {
      // this is unexpected
      logger.fine("context was never set");
      failDelegate(ex);
      return;
    }

    // end span before calling delegate
    helper().endInstrumentation(context, otelRequest, null, ex);

    if (parentContext == null) {
      failDelegate(ex);
      return;
    }

    try (Scope ignored = parentContext.makeCurrent()) {
      failDelegate(ex);
    }
  }

  @Override
  public void cancelled() {
    if (context == null) {
      // this is unexpected
      logger.fine("context was never set");
      cancelDelegate();
      return;
    }

    // TODO (trask) add "canceled" span attribute
    // end span before calling delegate
    helper().endInstrumentation(context, otelRequest, null, null);

    if (parentContext == null) {
      cancelDelegate();
      return;
    }

    try (Scope ignored = parentContext.makeCurrent()) {
      cancelDelegate();
    }
  }

  private void completeDelegate(T result) {
    if (delegate != null) {
      delegate.completed(result);
    }
  }

  private void failDelegate(Exception ex) {
    if (delegate != null) {
      delegate.failed(ex);
    }
  }

  private void cancelDelegate() {
    if (delegate != null) {
      delegate.cancelled();
    }
  }
}
