/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.semconv.http;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanStatusExtractor;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

public interface HttpServerTelemetryBuilder<REQUEST, RESPONSE> {

  @CanIgnoreReturnValue
  HttpServerTelemetryBuilder<REQUEST, RESPONSE> addAttributesExtractor(
      AttributesExtractor<REQUEST, RESPONSE> attributesExtractor);

  @CanIgnoreReturnValue
  HttpServerTelemetryBuilder<REQUEST, RESPONSE> setCapturedRequestHeaders(
      List<String> requestHeaders);

  @CanIgnoreReturnValue
  HttpServerTelemetryBuilder<REQUEST, RESPONSE> setCapturedResponseHeaders(
      List<String> responseHeaders);

  @CanIgnoreReturnValue
  HttpServerTelemetryBuilder<REQUEST, RESPONSE> setKnownMethods(Set<String> knownMethods);

  @CanIgnoreReturnValue
  HttpServerTelemetryBuilder<REQUEST, RESPONSE> setSpanNameExtractor(
      Function<SpanNameExtractor<REQUEST>, SpanNameExtractor<REQUEST>>
          spanNameExtractorTransformer);

  @CanIgnoreReturnValue
  HttpServerTelemetryBuilder<REQUEST, RESPONSE> setStatusExtractor(
      Function<SpanStatusExtractor<REQUEST, RESPONSE>, SpanStatusExtractor<REQUEST, RESPONSE>>
          statusExtractorTransformer);

  Object build();
}
