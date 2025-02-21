/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package com.example.javaagent;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

/**
 * Custom Span Processor to inject Baggage attributes to a Span. This allows to use the Baggage to
 * hold some custom values and inject them automatically into the child Spans.
 *
 * <p>See <a
 * href="https://github.com/open-telemetry/opentelemetry-specification/blob/master/specification/trace/sdk.md#span-processor">
 * OpenTelemetry Specification</a> for more information about {@link SpanProcessor}.
 *
 * @see DemoAutoConfigurationCustomizerProvider
 */
public class DemoBaggageSpanProcessor implements SpanProcessor {

  @Override
  public void onStart(Context parentContext, ReadWriteSpan span) {
    Baggage.fromContext(parentContext)
        .forEach((s, baggageEntry) -> span.setAttribute(s, baggageEntry.getValue()));
  }

  @Override
  public boolean isStartRequired() {
    return true;
  }

  @Override
  public void onEnd(ReadableSpan span) {
    // Do nothing on span end
  }

  @Override
  public boolean isEndRequired() {
    return false;
  }
}
