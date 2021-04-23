/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.instrumenter.http;

import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;

public final class HttpSpanNameExtractor<REQUEST> implements SpanNameExtractor<REQUEST> {

  /**
   * Returns a {@link SpanNameExtractor} which should be used for HTTP requests. HTTP attributes
   * will be examined to determine the name of the span.
   */
  public static <REQUEST> SpanNameExtractor<REQUEST> create(
      HttpAttributesExtractor<REQUEST, ?> attributesExtractor) {
    return new HttpSpanNameExtractor<>(attributesExtractor);
  }

  private final HttpAttributesExtractor<REQUEST, ?> attributesExtractor;

  private HttpSpanNameExtractor(HttpAttributesExtractor<REQUEST, ?> attributesExtractor) {
    this.attributesExtractor = attributesExtractor;
  }

  @Override
  public String extract(REQUEST request) {
    String route = attributesExtractor.route(request);
    if (route != null) {
      return route;
    }
    String method = attributesExtractor.method(request);
    if (method != null) {
      return "HTTP " + method;
    }
    return "HTTP request";
  }
}
