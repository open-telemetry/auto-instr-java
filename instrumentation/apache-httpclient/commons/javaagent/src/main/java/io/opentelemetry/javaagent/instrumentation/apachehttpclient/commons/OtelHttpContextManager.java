/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.commons;

import static io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge.currentContext;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.internal.SpanKey;
import java.util.function.Function;
import javax.annotation.Nullable;

public abstract class OtelHttpContextManager<CTX> {
  private final Function<CTX, OtelHttpContext> adapter;

  protected OtelHttpContextManager(Function<CTX, OtelHttpContext> adapter) {
    this.adapter = adapter;
  }

  public void setCurrentContext(CTX httpContext, Context context) {
    if (httpContext != null) {
      adapter.apply(httpContext).setContext(context);
    }
  }

  public void clearOtelAttributes(CTX httpContext) {
    if (httpContext != null) {
      adapter.apply(httpContext).clear();
    }
  }

  @Nullable
  public Context getCurrentContext(CTX httpContext) {
    if (httpContext == null) {
      return null;
    }
    OtelHttpContext otelHttpContext = adapter.apply(httpContext);
    Context otelContext = otelHttpContext.getContext();
    if (otelContext == null) {
      // for async clients, the contexts should always be set by their instrumentation
      if (otelHttpContext.isAsyncClient()) {
        return null;
      }
      // for classic clients, context will remain same as the caller
      otelContext = currentContext();
    }
    // verifying if the current context is a http client context
    // this eliminates suppressed contexts and http processor cases which ran for
    // apache http server also present in the library
    Span span = SpanKey.HTTP_CLIENT.fromContextOrNull(otelContext);
    if (span == null) {
      return null;
    }
    return otelContext;
  }
}
