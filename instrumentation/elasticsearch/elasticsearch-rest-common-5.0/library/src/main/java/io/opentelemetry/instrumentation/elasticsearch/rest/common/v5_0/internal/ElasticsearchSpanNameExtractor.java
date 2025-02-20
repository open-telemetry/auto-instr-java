/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.elasticsearch.rest.common.v5_0.internal;

import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public class ElasticsearchSpanNameExtractor implements SpanNameExtractor<ElasticsearchRestRequest> {

  private final ElasticsearchDbAttributesGetter dbAttributesGetter;

  public ElasticsearchSpanNameExtractor(ElasticsearchDbAttributesGetter dbAttributesGetter) {
    this.dbAttributesGetter = dbAttributesGetter;
  }

  @Override
  public String extract(ElasticsearchRestRequest elasticsearchRestRequest) {
    String name = dbAttributesGetter.getDbOperationName(elasticsearchRestRequest);
    return name != null ? name : elasticsearchRestRequest.getMethod();
  }
}
