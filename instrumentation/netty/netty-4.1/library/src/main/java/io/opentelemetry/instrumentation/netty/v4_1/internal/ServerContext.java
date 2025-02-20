/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.netty.v4_1.internal;

import com.google.auto.value.AutoValue;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.netty.common.v4_0.HttpRequestAndChannel;

/**
 * A tuple of an {@link Context} and a {@link HttpRequestAndChannel}.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
@AutoValue
public abstract class ServerContext {

  /** Create a new {@link ServerContext}. */
  public static ServerContext create(Context context, HttpRequestAndChannel request) {
    return new AutoValue_ServerContext(context, request);
  }

  /** Returns the {@link Context}. */
  public abstract Context context();

  /** Returns the {@link HttpRequestAndChannel}. */
  public abstract HttpRequestAndChannel request();
}
