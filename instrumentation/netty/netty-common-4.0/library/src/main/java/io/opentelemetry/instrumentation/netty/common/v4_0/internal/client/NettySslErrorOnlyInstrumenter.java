/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.netty.common.v4_0.internal.client;

import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.internal.InstrumenterUtil;
import io.opentelemetry.instrumentation.netty.common.internal.Timer;
import javax.annotation.Nullable;

final class NettySslErrorOnlyInstrumenter implements NettySslInstrumenter {

  private final Instrumenter<NettySslRequest, Void> instrumenter;

  NettySslErrorOnlyInstrumenter(Instrumenter<NettySslRequest, Void> instrumenter) {
    this.instrumenter = instrumenter;
  }

  @Override
  public boolean shouldStart(Context parentContext, NettySslRequest request) {
    // the "real" check is done on end() anyway
    return true;
  }

  @Override
  public Context start(Context parentContext, NettySslRequest request) {
    return parentContext.with(Timer.start());
  }

  @Override
  public void end(Context context, NettySslRequest request, @Nullable Throwable error) {
    if (error != null && instrumenter.shouldStart(context, request)) {
      Timer timer = Timer.get(context);
      InstrumenterUtil.startAndEnd(
          instrumenter, context, request, null, error, timer.startTime(), timer.now());
    }
  }
}
