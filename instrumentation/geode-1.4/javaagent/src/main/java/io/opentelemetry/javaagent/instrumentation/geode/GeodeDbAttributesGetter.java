/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.geode;

import io.opentelemetry.instrumentation.api.incubator.semconv.db.DbClientAttributesGetter;
import io.opentelemetry.instrumentation.api.incubator.semconv.db.SqlStatementSanitizer;
import io.opentelemetry.javaagent.bootstrap.internal.AgentCommonConfig;
import io.opentelemetry.semconv.incubating.DbIncubatingAttributes;
import javax.annotation.Nullable;

final class GeodeDbAttributesGetter implements DbClientAttributesGetter<GeodeRequest, Void> {

  private static final SqlStatementSanitizer sanitizer =
      SqlStatementSanitizer.create(AgentCommonConfig.get().isStatementSanitizationEnabled());

  @SuppressWarnings("deprecation") // using deprecated DbSystemIncubatingValues
  @Override
  public String getDbSystem(GeodeRequest request) {
    return DbIncubatingAttributes.DbSystemIncubatingValues.GEODE;
  }

  @Deprecated
  @Override
  @Nullable
  public String getUser(GeodeRequest request) {
    return null;
  }

  @Override
  @Nullable
  public String getDbNamespace(GeodeRequest request) {
    return request.getRegion().getName();
  }

  @Deprecated
  @Override
  @Nullable
  public String getConnectionString(GeodeRequest request) {
    return null;
  }

  @Override
  @Nullable
  public String getDbQueryText(GeodeRequest request) {
    // sanitized statement is cached
    return sanitizer.sanitize(request.getQuery()).getFullStatement();
  }

  @Override
  @Nullable
  public String getDbOperationName(GeodeRequest request) {
    return request.getOperation();
  }
}
