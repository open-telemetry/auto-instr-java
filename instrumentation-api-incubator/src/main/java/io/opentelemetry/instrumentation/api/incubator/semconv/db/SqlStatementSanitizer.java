/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.incubator.semconv.db;

import static io.opentelemetry.instrumentation.api.internal.SupportabilityMetrics.CounterNames.SQL_STATEMENT_SANITIZER_CACHE_MISS;

import com.google.auto.value.AutoValue;
import io.opentelemetry.instrumentation.api.internal.SupportabilityMetrics;
import io.opentelemetry.instrumentation.api.internal.cache.Cache;
import javax.annotation.Nullable;

/**
 * This class is responsible for masking potentially sensitive parameters in SQL (and SQL-like)
 * statements and queries.
 */
public final class SqlStatementSanitizer {
  private static final SupportabilityMetrics supportability = SupportabilityMetrics.instance();

  private static final Cache<CacheKey, SqlStatementInfo> sqlToStatementInfoCache =
      Cache.bounded(1000);
  private static final int LARGE_STATEMENT_THRESHOLD = 10 * 1024;

  public static SqlStatementSanitizer create(boolean statementSanitizationEnabled) {
    return new SqlStatementSanitizer(statementSanitizationEnabled);
  }

  private final boolean statementSanitizationEnabled;

  private SqlStatementSanitizer(boolean statementSanitizationEnabled) {
    this.statementSanitizationEnabled = statementSanitizationEnabled;
  }

  public SqlStatementInfo sanitize(@Nullable String statement) {
    return sanitize(statement, SqlDialect.DEFAULT);
  }

  public SqlStatementInfo sanitize(@Nullable String statement, SqlDialect dialect) {
    if (!statementSanitizationEnabled || statement == null) {
      return SqlStatementInfo.create(statement, null, null);
    }
    // sanitization result will not be cached for statements larger than the threshold to avoid
    // cache growing too large
    // https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/13180
    if (statement.length() > LARGE_STATEMENT_THRESHOLD) {
      return sanitizeImpl(statement, dialect);
    }
    return sqlToStatementInfoCache.computeIfAbsent(
        CacheKey.create(statement, dialect), k -> sanitizeImpl(statement, dialect));
  }

  private static SqlStatementInfo sanitizeImpl(@Nullable String statement, SqlDialect dialect) {
    supportability.incrementCounter(SQL_STATEMENT_SANITIZER_CACHE_MISS);
    return AutoSqlSanitizer.sanitize(statement, dialect);
  }

  // visible for tests
  static boolean isCached(String statement) {
    return sqlToStatementInfoCache.get(CacheKey.create(statement, SqlDialect.DEFAULT)) != null;
  }

  @AutoValue
  abstract static class CacheKey {

    static CacheKey create(String statement, SqlDialect dialect) {
      return new AutoValue_SqlStatementSanitizer_CacheKey(statement, dialect);
    }

    abstract String getStatement();

    abstract SqlDialect getDialect();
  }
}
