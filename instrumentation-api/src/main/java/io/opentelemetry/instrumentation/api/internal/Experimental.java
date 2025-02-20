/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.internal;

import io.opentelemetry.instrumentation.api.semconv.http.HttpClientAttributesExtractorBuilder;
import java.util.function.BiConsumer;
import javax.annotation.Nullable;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class Experimental {

  @Nullable
  private static volatile BiConsumer<HttpClientAttributesExtractorBuilder<?, ?>, Boolean>
      redactHttpClientQueryParameters;

  private Experimental() {}

  public static void setRedactQueryParameters(
      HttpClientAttributesExtractorBuilder<?, ?> builder, boolean redactQueryParameters) {
    if (redactHttpClientQueryParameters != null) {
      redactHttpClientQueryParameters.accept(builder, redactQueryParameters);
    }
  }

  public static void internalSetRedactHttpClientQueryParameters(
      BiConsumer<HttpClientAttributesExtractorBuilder<?, ?>, Boolean>
          redactHttpClientQueryParameters) {
    Experimental.redactHttpClientQueryParameters = redactHttpClientQueryParameters;
  }
}
