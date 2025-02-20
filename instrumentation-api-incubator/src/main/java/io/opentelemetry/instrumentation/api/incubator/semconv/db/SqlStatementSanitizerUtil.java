/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.incubator.semconv.db;

import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.internal.InstrumenterContext;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class for sanitizing sql that keeps sanitization results in {@link InstrumenterContext} so
 * that each statement would be sanitized only once for given {@link Instrumenter} call.
 */
class SqlStatementSanitizerUtil {
  private static final SqlStatementSanitizer sanitizer = SqlStatementSanitizer.create(true);

  static SqlStatementInfo sanitize(String queryText) {
    if (!InstrumenterContext.isActive()) {
      return sanitizer.sanitize(queryText);
    }

    Map<String, SqlStatementInfo> map =
        InstrumenterContext.computeIfAbsent("sanitized-sql-map", unused -> new HashMap<>());
    return map.computeIfAbsent(queryText, sanitizer::sanitize);
  }

  private SqlStatementSanitizerUtil() {}
}
