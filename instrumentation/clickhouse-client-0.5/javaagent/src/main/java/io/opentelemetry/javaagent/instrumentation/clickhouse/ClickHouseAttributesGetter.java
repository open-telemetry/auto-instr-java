/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.clickhouse;

import io.opentelemetry.instrumentation.api.incubator.semconv.db.DbClientAttributesGetter;
import io.opentelemetry.semconv.incubating.DbIncubatingAttributes;
import javax.annotation.Nullable;

final class ClickHouseAttributesGetter implements DbClientAttributesGetter<ClickHouseDbRequest> {
  @Deprecated
  @Nullable
  @Override
  public String getStatement(ClickHouseDbRequest request) {
    if (request.getSqlStatementInfo() == null) {
      return null;
    }
    return request.getSqlStatementInfo().getFullStatement();
  }

  @Nullable
  @Override
  public String getDbQueryText(ClickHouseDbRequest request) {
    if (request.getSqlStatementInfo() == null) {
      return null;
    }
    return request.getSqlStatementInfo().getFullStatement();
  }

  @Deprecated
  @Nullable
  @Override
  public String getOperation(ClickHouseDbRequest request) {
    if (request.getSqlStatementInfo() == null) {
      return null;
    }
    return request.getSqlStatementInfo().getOperation();
  }

  @Nullable
  @Override
  public String getDbOperationName(ClickHouseDbRequest request) {
    if (request.getSqlStatementInfo() == null) {
      return null;
    }
    return request.getSqlStatementInfo().getOperation();
  }

  @Nullable
  @Override
  public String getSystem(ClickHouseDbRequest request) {
    return DbIncubatingAttributes.DbSystemValues.CLICKHOUSE;
  }

  @Deprecated
  @Nullable
  @Override
  public String getUser(ClickHouseDbRequest request) {
    return null;
  }

  @Deprecated
  @Nullable
  @Override
  public String getName(ClickHouseDbRequest request) {
    String dbName = request.getDbName();
    if (dbName == null || dbName.isEmpty()) {
      return null;
    }
    return dbName;
  }

  @Nullable
  @Override
  public String getDbNamespace(ClickHouseDbRequest request) {
    return getName(request);
  }

  @Deprecated
  @Nullable
  @Override
  public String getConnectionString(ClickHouseDbRequest request) {
    return null;
  }
}
