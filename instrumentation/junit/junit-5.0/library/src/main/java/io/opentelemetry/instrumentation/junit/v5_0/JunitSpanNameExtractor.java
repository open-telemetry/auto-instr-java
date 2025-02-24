/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.junit.v5_0;

import static io.opentelemetry.instrumentation.junit.v5_0.JunitConstants.JUNIT_SPAN_NAME;
import static io.opentelemetry.instrumentation.junit.v5_0.JunitConstants.OPENTELEMETRY_TRACING_EXTENSION_NAMESPACE;

import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import java.util.Optional;
import org.junit.jupiter.api.extension.ExtensionContext;

class JunitSpanNameExtractor implements SpanNameExtractor<ExtensionContext> {

  @Override
  public String extract(ExtensionContext extensionContext) {
    return Optional.ofNullable(
            extensionContext
                .getStore(OPENTELEMETRY_TRACING_EXTENSION_NAMESPACE)
                .get(JUNIT_SPAN_NAME, String.class))
        .orElseGet(extensionContext::getDisplayName);
  }
}
