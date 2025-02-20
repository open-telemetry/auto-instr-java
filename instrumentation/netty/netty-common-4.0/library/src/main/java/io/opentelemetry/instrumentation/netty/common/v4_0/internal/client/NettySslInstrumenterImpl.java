/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.netty.common.v4_0.internal.client;

import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import javax.annotation.Nullable;

final class NettySslInstrumenterImpl implements NettySslInstrumenter {

  private final Instrumenter<NettySslRequest, Void> instrumenter;

  NettySslInstrumenterImpl(Instrumenter<NettySslRequest, Void> instrumenter) {
    this.instrumenter = instrumenter;
  }

  @Override
  public boolean shouldStart(Context parentContext, NettySslRequest request) {
    return instrumenter.shouldStart(parentContext, request);
  }

  @Override
  public Context start(Context parentContext, NettySslRequest request) {
    return instrumenter.start(parentContext, request);
  }

  @Override
  public void end(Context context, NettySslRequest request, @Nullable Throwable error) {
    instrumenter.end(context, request, null, error);
  }
}
