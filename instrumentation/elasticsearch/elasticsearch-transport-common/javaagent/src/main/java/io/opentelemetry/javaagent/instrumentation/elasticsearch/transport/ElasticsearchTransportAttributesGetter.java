/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.elasticsearch.transport;

import io.opentelemetry.instrumentation.api.incubator.semconv.db.DbClientAttributesGetter;
import io.opentelemetry.semconv.incubating.DbIncubatingAttributes;
import javax.annotation.Nullable;
import org.elasticsearch.action.ActionResponse;

final class ElasticsearchTransportAttributesGetter
    implements DbClientAttributesGetter<ElasticTransportRequest, ActionResponse> {

  @SuppressWarnings("deprecation") // using deprecated DbSystemIncubatingValues
  @Override
  public String getDbSystem(ElasticTransportRequest request) {
    return DbIncubatingAttributes.DbSystemIncubatingValues.ELASTICSEARCH;
  }

  @Deprecated
  @Override
  @Nullable
  public String getUser(ElasticTransportRequest request) {
    return null;
  }

  @Override
  @Nullable
  public String getDbNamespace(ElasticTransportRequest request) {
    return null;
  }

  @Deprecated
  @Override
  @Nullable
  public String getConnectionString(ElasticTransportRequest request) {
    return null;
  }

  @Override
  @Nullable
  public String getDbQueryText(ElasticTransportRequest request) {
    return null;
  }

  @Override
  public String getDbOperationName(ElasticTransportRequest request) {
    return request.getAction().getClass().getSimpleName();
  }
}
