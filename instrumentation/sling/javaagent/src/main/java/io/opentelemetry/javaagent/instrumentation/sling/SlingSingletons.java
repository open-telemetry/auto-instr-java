/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.sling;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import org.apache.sling.api.SlingHttpServletRequest;

public final class SlingSingletons {
  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.sling-1.0";

  static final String REQUEST_ATTR_RESOLVED_SERVLET_NAME =
      INSTRUMENTATION_NAME + ".resolvedServletName";

  private static final SpanNameExtractor<SlingHttpServletRequest> SPAN_NAME_EXTRACTOR =
      s -> (String) s.getAttribute(REQUEST_ATTR_RESOLVED_SERVLET_NAME);
  private static final Instrumenter<SlingHttpServletRequest, Void> INSTRUMENTER =
      Instrumenter.<SlingHttpServletRequest, Void>builder(
              GlobalOpenTelemetry.get(), INSTRUMENTATION_NAME, SPAN_NAME_EXTRACTOR)
          .buildInstrumenter();

  public static Instrumenter<SlingHttpServletRequest, Void> helper() {
    return INSTRUMENTER;
  }

  private SlingSingletons() {}
}
