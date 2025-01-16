/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.aerospike.internal;

import static io.opentelemetry.javaagent.instrumentation.aerospike.AerospikeSingletons.instrumenter;

import io.opentelemetry.context.Context;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class AerospikeRequestContext {
  private static final ThreadLocal<AerospikeRequestContext> contextThreadLocal =
      new ThreadLocal<>();
  private AerospikeRequest request;
  private Context context;
  private Throwable throwable;

  private AerospikeRequestContext() {}

  public static AerospikeRequestContext attach(AerospikeRequest request, Context context) {
    AerospikeRequestContext requestContext = current();
    if (requestContext != null) {
      requestContext.detachContext();
    }
    requestContext = new AerospikeRequestContext();
    requestContext.request = request;
    requestContext.context = context;
    contextThreadLocal.set(requestContext);
    return requestContext;
  }

  public void detachAndEnd() {
    detachContext();
    if (request != null) {
      endSpan();
    }
  }

  public void detachContext() {
    contextThreadLocal.remove();
  }

  public static AerospikeRequestContext current() {
    return contextThreadLocal.get();
  }

  public void endSpan() {
    instrumenter().end(context, request, null, throwable);
  }

  public AerospikeRequest getRequest() {
    return request;
  }

  public void setThrowable(Throwable throwable) {
    this.throwable = throwable;
  }
}
