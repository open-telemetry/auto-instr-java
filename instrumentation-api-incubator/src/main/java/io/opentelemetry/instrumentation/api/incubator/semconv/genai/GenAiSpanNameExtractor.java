/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.incubator.semconv.genai;

import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;

/** A {@link SpanNameExtractor} for GenAI requests. */
public final class GenAiSpanNameExtractor<REQUEST> implements SpanNameExtractor<REQUEST> {

  /**
   * Returns a {@link SpanNameExtractor} that constructs the span name according to GenAI semantic
   * conventions: {@code <gen_ai.operation.name> <gen_ai.request.model>}.
   */
  public static <REQUEST> SpanNameExtractor<REQUEST> create(
      GenAiAttributesGetter<REQUEST, ?> attributesGetter) {
    return new GenAiSpanNameExtractor<>(attributesGetter);
  }

  private final GenAiAttributesGetter<REQUEST, ?> getter;

  private GenAiSpanNameExtractor(GenAiAttributesGetter<REQUEST, ?> getter) {
    this.getter = getter;
  }

  @Override
  public String extract(REQUEST request) {
    String operation = getter.getOperationName(request);
    String model = getter.getRequestModel(request);
    if (model == null) {
      return operation;
    }
    return operation + ' ' + model;
  }
}
