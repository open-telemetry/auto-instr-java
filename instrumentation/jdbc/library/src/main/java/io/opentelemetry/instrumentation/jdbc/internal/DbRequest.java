/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.jdbc.internal;

import static io.opentelemetry.instrumentation.jdbc.internal.JdbcUtils.connectionFromStatement;
import static io.opentelemetry.instrumentation.jdbc.internal.JdbcUtils.extractDbInfo;

import com.google.auto.value.AutoValue;
import io.opentelemetry.instrumentation.jdbc.internal.dbinfo.DbInfo;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Statement;
import javax.annotation.Nullable;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
@AutoValue
public abstract class DbRequest {

  @Nullable
  public static DbRequest create(PreparedStatement statement) {
    return create(statement, JdbcData.preparedStatement.get(statement));
  }

  @Nullable
  public static DbRequest create(Statement statement, String dbStatementString) {
    Connection connection = connectionFromStatement(statement);
    if (connection == null) {
      return null;
    }

    return create(extractDbInfo(connection), dbStatementString);
  }

  public static DbRequest create(DbInfo dbInfo, String queryText) {
    return new AutoValue_DbRequest(dbInfo, queryText);
  }

  public abstract DbInfo getDbInfo();

  public abstract String getQueryText();
}
