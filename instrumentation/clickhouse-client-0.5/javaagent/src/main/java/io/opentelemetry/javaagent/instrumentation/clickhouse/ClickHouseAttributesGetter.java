/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.clickhouse;

import io.opentelemetry.instrumentation.api.incubator.semconv.db.DbClientAttributesGetter;
import io.opentelemetry.semconv.incubating.DbIncubatingAttributes;
import javax.annotation.Nullable;

final class ClickHouseAttributesGetter implements DbClientAttributesGetter<ClickHouseDbRequest, Void> {

  @Nullable
  @Override
  public String getDbQueryText(ClickHouseDbRequest request) {
    if (request.getSqlStatementInfo() == null) {
      return null;
    }
    return request.getSqlStatementInfo().getFullStatement();
  }

  @Nullable
  @Override
  public String getDbOperationName(ClickHouseDbRequest request) {
    if (request.getSqlStatementInfo() == null) {
      return null;
    }
    return request.getSqlStatementInfo().getOperation();
  }

  @SuppressWarnings("deprecation") // using deprecated DbSystemIncubatingValues
  @Override
  public String getDbSystem(ClickHouseDbRequest request) {
    return DbIncubatingAttributes.DbSystemIncubatingValues.CLICKHOUSE;
  }

  @Deprecated
  @Nullable
  @Override
  public String getUser(ClickHouseDbRequest request) {
    return null;
  }

  @Nullable
  @Override
  public String getDbNamespace(ClickHouseDbRequest request) {
    String dbName = request.getDbName();
    if (dbName == null || dbName.isEmpty()) {
      return null;
    }
    return dbName;
  }

  @Deprecated
  @Nullable
  @Override
  public String getConnectionString(ClickHouseDbRequest request) {
    return null;
  }
}
