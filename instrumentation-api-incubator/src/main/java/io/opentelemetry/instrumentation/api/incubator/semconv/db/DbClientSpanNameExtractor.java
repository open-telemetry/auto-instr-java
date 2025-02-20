/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.incubator.semconv.db;

import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.opentelemetry.instrumentation.api.internal.SemconvStability;
import java.util.Collection;

public abstract class DbClientSpanNameExtractor<REQUEST> implements SpanNameExtractor<REQUEST> {

  /**
   * Returns a {@link SpanNameExtractor} that constructs the span name according to DB semantic
   * conventions: {@code <db.operation> <db.name>}.
   *
   * @see DbClientAttributesGetter#getDbOperationName(Object) used to extract {@code
   *     <db.operation.name>}.
   * @see DbClientAttributesGetter#getDbNamespace(Object) used to extract {@code <db.namespace>}.
   */
  public static <REQUEST> SpanNameExtractor<REQUEST> create(
      DbClientAttributesGetter<REQUEST> getter) {
    return new GenericDbClientSpanNameExtractor<>(getter);
  }

  /**
   * Returns a {@link SpanNameExtractor} that constructs the span name according to DB semantic
   * conventions: {@code <db.operation> <db.name>.<identifier>}.
   *
   * @see SqlStatementInfo#getOperation() used to extract {@code <db.operation>}.
   * @see DbClientAttributesGetter#getDbNamespace(Object) used to extract {@code <db.namespace>}.
   * @see SqlStatementInfo#getMainIdentifier() used to extract {@code <db.table>} or stored
   *     procedure name.
   */
  public static <REQUEST> SpanNameExtractor<REQUEST> create(
      SqlClientAttributesGetter<REQUEST> getter) {
    return new SqlClientSpanNameExtractor<>(getter);
  }

  private static final String DEFAULT_SPAN_NAME = "DB Query";

  private DbClientSpanNameExtractor() {}

  protected String computeSpanName(String dbName, String operation, String mainIdentifier) {
    if (operation == null) {
      return dbName == null ? DEFAULT_SPAN_NAME : dbName;
    }

    StringBuilder name = new StringBuilder(operation);
    if (dbName != null || mainIdentifier != null) {
      name.append(' ');
    }
    // skip db name if identifier already has a db name prefixed to it
    if (dbName != null && (mainIdentifier == null || mainIdentifier.indexOf('.') == -1)) {
      name.append(dbName);
      if (mainIdentifier != null) {
        name.append('.');
      }
    }
    if (mainIdentifier != null) {
      name.append(mainIdentifier);
    }
    return name.toString();
  }

  private static final class GenericDbClientSpanNameExtractor<REQUEST>
      extends DbClientSpanNameExtractor<REQUEST> {

    private final DbClientAttributesGetter<REQUEST> getter;

    private GenericDbClientSpanNameExtractor(DbClientAttributesGetter<REQUEST> getter) {
      this.getter = getter;
    }

    @Override
    public String extract(REQUEST request) {
      String namespace = getter.getDbNamespace(request);
      String operationName = getter.getDbOperationName(request);
      return computeSpanName(namespace, operationName, null);
    }
  }

  private static final class SqlClientSpanNameExtractor<REQUEST>
      extends DbClientSpanNameExtractor<REQUEST> {

    private final SqlClientAttributesGetter<REQUEST> getter;

    private SqlClientSpanNameExtractor(SqlClientAttributesGetter<REQUEST> getter) {
      this.getter = getter;
    }

    @Override
    public String extract(REQUEST request) {
      String namespace = getter.getDbNamespace(request);
      Collection<String> rawQueryTexts = getter.getRawQueryTexts(request);

      if (rawQueryTexts.isEmpty()) {
        return computeSpanName(namespace, null, null);
      }

      if (!SemconvStability.emitStableDatabaseSemconv()) {
        if (rawQueryTexts.size() > 1) { // for backcompat(?)
          return computeSpanName(namespace, null, null);
        }
        SqlStatementInfo sanitizedStatement =
            SqlStatementSanitizerUtil.sanitize(rawQueryTexts.iterator().next());
        return computeSpanName(
            namespace, sanitizedStatement.getOperation(), sanitizedStatement.getMainIdentifier());
      }

      if (rawQueryTexts.size() == 1) {
        SqlStatementInfo sanitizedStatement =
            SqlStatementSanitizerUtil.sanitize(rawQueryTexts.iterator().next());
        String operation = sanitizedStatement.getOperation();
        if (isBatch(request)) {
          operation = "BATCH " + operation;
        }
        return computeSpanName(namespace, operation, sanitizedStatement.getMainIdentifier());
      }

      MultiQuery multiQuery = MultiQuery.analyze(rawQueryTexts, false);
      return computeSpanName(
          namespace,
          multiQuery.getOperation() != null ? "BATCH " + multiQuery.getOperation() : "BATCH",
          multiQuery.getMainIdentifier());
    }

    private boolean isBatch(REQUEST request) {
      Long batchSize = getter.getBatchSize(request);
      return batchSize != null && batchSize > 1;
    }
  }
}
