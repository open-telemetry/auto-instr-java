/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.jms;

import static io.opentelemetry.instrumentation.api.internal.SemconvStability.emitStableMessagingSemconv;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.internal.InstrumenterUtil;
import io.opentelemetry.instrumentation.api.internal.Timer;
import io.opentelemetry.javaagent.bootstrap.internal.ExperimentalConfig;
import io.opentelemetry.javaagent.bootstrap.jms.JmsReceiveContextHolder;

public final class JmsReceiveSpanUtil {
  private static final ContextPropagators propagators = GlobalOpenTelemetry.getPropagators();
  private static final boolean receiveInstrumentationEnabled =
      ExperimentalConfig.get().messagingReceiveInstrumentationEnabled();

  public static void createReceiveSpan(
      Instrumenter<MessageWithDestination, Void> receiveInstrumenter,
      MessageWithDestination request,
      Timer timer,
      Throwable throwable) {
    Context parentContext = Context.current();
    // if receive instrumentation is not enabled we'll use the producer as parent
    // according to the stable convertions the production should only be linked not as parent
    if (!receiveInstrumentationEnabled && !emitStableMessagingSemconv()) {
      parentContext =
          propagators
              .getTextMapPropagator()
              .extract(parentContext, request, MessagePropertyGetter.INSTANCE);
    }

    if (receiveInstrumenter.shouldStart(parentContext, request)) {
      Context receiveContext =
          InstrumenterUtil.startAndEnd(
              receiveInstrumenter,
              parentContext,
              request,
              null,
              throwable,
              timer.startTime(),
              timer.now());
      JmsReceiveContextHolder.set(receiveContext);
    }
  }

  private JmsReceiveSpanUtil() {}
}
