/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.semconv.url;

import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.semconv.url.internal.InternalUrlAttributesExtractor;
import javax.annotation.Nullable;

/**
 * Extractor of <a
 * href="https://github.com/open-telemetry/semantic-conventions/blob/v1.23.0/docs/url/url.md">URL
 * attributes</a>.
 *
 * @since 2.0.0
 */
public final class UrlAttributesExtractor<REQUEST, RESPONSE>
    implements AttributesExtractor<REQUEST, RESPONSE> {

  /**
   * Creates the URL attributes extractor.
   *
   * @see InstrumenterBuilder#addAttributesExtractor(AttributesExtractor)
   */
  public static <REQUEST, RESPONSE> UrlAttributesExtractor<REQUEST, RESPONSE> create(
      UrlAttributesGetter<REQUEST> getter) {
    return new UrlAttributesExtractor<>(getter);
  }

  private final InternalUrlAttributesExtractor<REQUEST> internalExtractor;

  UrlAttributesExtractor(UrlAttributesGetter<REQUEST> getter) {
    // the UrlAttributesExtractor will always emit new semconv
    internalExtractor =
        new InternalUrlAttributesExtractor<>(
            getter, /* alternateSchemeProvider= */ request -> null);
  }

  @Override
  public void onStart(AttributesBuilder attributes, Context parentContext, REQUEST request) {}

  @Override
  public void onEnd(
      AttributesBuilder attributes,
      Context context,
      REQUEST request,
      @Nullable RESPONSE response,
      @Nullable Throwable error) {
    internalExtractor.onEnd(attributes, request);
  }
}
