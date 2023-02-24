/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.commons;

import io.opentelemetry.context.Context;
import java.util.Objects;

public abstract class OtelHttpContext {
  private static final String CONTEXT_ATTRIBUTE = "@otel.context";
  private static final String ASYNC_CLIENT_ATTRIBUTE = "@otel.async.client";

  protected abstract <T> void setAttribute(String name, T value);

  protected abstract <T> T getAttribute(String name, Class<T> type);

  protected abstract void removeAttribute(String name);

  public void setContext(Context context) {
    setAttribute(CONTEXT_ATTRIBUTE, context);
  }

  public void markAsyncClient() {
    setAttribute(ASYNC_CLIENT_ATTRIBUTE, true);
  }

  public Context getContext() {
    return getAttribute(CONTEXT_ATTRIBUTE, Context.class);
  }

  public boolean isAsyncClient() {
    return Objects.equals(getAttribute(ASYNC_CLIENT_ATTRIBUTE, Boolean.class), Boolean.TRUE);
  }

  public void clear() {
    removeAttribute(CONTEXT_ATTRIBUTE);
    removeAttribute(ASYNC_CLIENT_ATTRIBUTE);
  }
}
