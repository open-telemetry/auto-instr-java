/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.netty.common.v4_0.internal.client;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.context.Context;
import javax.annotation.Nullable;

enum NoopSslInstrumenter implements NettySslInstrumenter {
  INSTANCE;

  @Override
  public boolean shouldStart(Context parentContext, NettySslRequest request) {
    return false;
  }

  @CanIgnoreReturnValue
  @Override
  public Context start(Context parentContext, NettySslRequest request) {
    return parentContext;
  }

  @Override
  public void end(Context context, NettySslRequest request, @Nullable Throwable error) {}
}
