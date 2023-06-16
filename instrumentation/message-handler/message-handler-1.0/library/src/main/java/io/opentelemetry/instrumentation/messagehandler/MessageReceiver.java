/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.messagehandler;

import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.internal.InstrumenterUtil;
import java.time.Instant;

public abstract class MessageReceiver<REQEUST, RESPONSE> {

  protected abstract Instrumenter<RESPONSE, Void> getMessageInstrumenter();

  protected abstract RESPONSE doReceive(REQEUST request);

  public RESPONSE receive(REQEUST request) {
    Instrumenter<RESPONSE, Void> instrumenter = getMessageInstrumenter();
    Context parentContext = Context.current();
    Throwable error = null;
    RESPONSE response = null;

    Instant startTime = Instant.now();
    try {
      response = doReceive(request);
    } catch (Throwable t) {
      error = t;
      throw t;
    } finally {
      InstrumenterUtil.startAndEnd(
          instrumenter, parentContext, response, null, error, startTime, Instant.now());
    }

    return response;
  }
}
