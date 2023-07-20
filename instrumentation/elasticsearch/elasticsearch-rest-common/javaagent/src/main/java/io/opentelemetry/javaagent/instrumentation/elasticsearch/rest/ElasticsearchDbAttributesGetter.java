/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.elasticsearch.rest;

import static java.util.logging.Level.FINE;

import io.opentelemetry.instrumentation.api.instrumenter.db.DbClientAttributesGetter;
import io.opentelemetry.javaagent.bootstrap.internal.InstrumentationConfig;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.apache.http.HttpEntity;

final class ElasticsearchDbAttributesGetter
    implements DbClientAttributesGetter<ElasticsearchRestRequest> {

  private static final boolean CAPTURE_SEARCH_QUERY =
      InstrumentationConfig.get()
          .getBoolean("otel.instrumentation.elasticsearch.capture-search-query", false);

  private static final Logger logger =
      Logger.getLogger(ElasticsearchDbAttributesGetter.class.getName());

  @Override
  public String getSystem(ElasticsearchRestRequest request) {
    return SemanticAttributes.DbSystemValues.ELASTICSEARCH;
  }

  @Override
  @Nullable
  public String getUser(ElasticsearchRestRequest request) {
    return null;
  }

  @Override
  @Nullable
  public String getName(ElasticsearchRestRequest request) {
    return null;
  }

  @Override
  @Nullable
  public String getConnectionString(ElasticsearchRestRequest request) {
    return null;
  }

  @Override
  @Nullable
  public String getStatement(ElasticsearchRestRequest request) {
    ElasticsearchEndpointDefinition epDefinition = request.getEndpointDefinition();
    HttpEntity httpEntity = request.getHttpEntity();
    if (CAPTURE_SEARCH_QUERY
        && epDefinition != null
        && epDefinition.isSearchEndpoint()
        && httpEntity != null
        && httpEntity.isRepeatable()) {
      // Retrieve HTTP body for search-type Elasticsearch requests when CAPTURE_SEARCH_QUERY is
      // enabled.
      try {
        return new BufferedReader(
                new InputStreamReader(httpEntity.getContent(), StandardCharsets.UTF_8))
            .lines()
            .collect(Collectors.joining());
      } catch (IOException e) {
        logger.log(FINE, "Failed reading HTTP body content.", e);
      }
    }
    return null;
  }

  @Override
  @Nullable
  public String getOperation(ElasticsearchRestRequest request) {
    ElasticsearchEndpointDefinition endpointDefinition = request.getEndpointDefinition();
    return endpointDefinition != null ? endpointDefinition.getEndpointName() : null;
  }
}
