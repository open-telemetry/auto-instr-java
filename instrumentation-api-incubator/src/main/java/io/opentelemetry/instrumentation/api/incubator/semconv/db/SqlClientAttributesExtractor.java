/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.incubator.semconv.db;

import static io.opentelemetry.instrumentation.api.internal.AttributesExtractorUtil.internalSet;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.internal.SemconvStability;
import java.util.Collection;

/**
 * Extractor of <a
 * href="https://github.com/open-telemetry/semantic-conventions/blob/main/docs/database/database-spans.md">database
 * attributes</a>. This class is designed with SQL (or SQL-like) database clients in mind.
 *
 * <p>It sets the same set of attributes as {@link DbClientAttributesExtractor} plus an additional
 * <code>db.sql.table</code> attribute. The raw SQL statements returned by the {@link
 * SqlClientAttributesGetter#getRawQueryText(Object)} method are sanitized before use, all statement
 * parameters are removed.
 */
public final class SqlClientAttributesExtractor<REQUEST, RESPONSE>
    extends DbClientCommonAttributesExtractor<
        REQUEST, RESPONSE, SqlClientAttributesGetter<REQUEST, RESPONSE>> {

  // copied from DbIncubatingAttributes
  private static final AttributeKey<String> DB_OPERATION = AttributeKey.stringKey("db.operation");
  private static final AttributeKey<String> DB_OPERATION_NAME =
      AttributeKey.stringKey("db.operation.name");
  private static final AttributeKey<String> DB_STATEMENT = AttributeKey.stringKey("db.statement");
  private static final AttributeKey<String> DB_QUERY_TEXT = AttributeKey.stringKey("db.query.text");
  static final AttributeKey<String> DB_COLLECTION_NAME =
      AttributeKey.stringKey("db.collection.name");
  private static final AttributeKey<Long> DB_OPERATION_BATCH_SIZE =
      AttributeKey.longKey("db.operation.batch.size");

  /** Creates the SQL client attributes extractor with default configuration. */
  public static <REQUEST, RESPONSE> AttributesExtractor<REQUEST, RESPONSE> create(
      SqlClientAttributesGetter<REQUEST, RESPONSE> getter) {
    return SqlClientAttributesExtractor.<REQUEST, RESPONSE>builder(getter).build();
  }

  /**
   * Returns a new {@link SqlClientAttributesExtractorBuilder} that can be used to configure the SQL
   * client attributes extractor.
   */
  public static <REQUEST, RESPONSE> SqlClientAttributesExtractorBuilder<REQUEST, RESPONSE> builder(
      SqlClientAttributesGetter<REQUEST, RESPONSE> getter) {
    return new SqlClientAttributesExtractorBuilder<>(getter);
  }

  private static final String SQL_CALL = "CALL";
  // sanitizer is also used to extract operation and table name, so we have it always enabled here
  private static final SqlStatementSanitizer sanitizer = SqlStatementSanitizer.create(true);

  private final AttributeKey<String> oldSemconvTableAttribute;
  private final boolean statementSanitizationEnabled;

  SqlClientAttributesExtractor(
      SqlClientAttributesGetter<REQUEST, RESPONSE> getter,
      AttributeKey<String> oldSemconvTableAttribute,
      boolean statementSanitizationEnabled) {
    super(getter);
    this.oldSemconvTableAttribute = oldSemconvTableAttribute;
    this.statementSanitizationEnabled = statementSanitizationEnabled;
  }

  @Override
  public void onStart(AttributesBuilder attributes, Context parentContext, REQUEST request) {
    super.onStart(attributes, parentContext, request);

    Collection<String> rawQueryTexts = getter.getRawQueryTexts(request);

    if (rawQueryTexts.isEmpty()) {
      return;
    }

    if (SemconvStability.emitOldDatabaseSemconv()) {
      if (rawQueryTexts.size() == 1) { // for backcompat(?)
        String rawQueryText = rawQueryTexts.iterator().next();
        SqlStatementInfo sanitizedStatement = sanitizer.sanitize(rawQueryText);
        String operation = sanitizedStatement.getOperation();
        internalSet(
            attributes,
            DB_STATEMENT,
            statementSanitizationEnabled ? sanitizedStatement.getFullStatement() : rawQueryText);
        internalSet(attributes, DB_OPERATION, operation);
        if (!SQL_CALL.equals(operation)) {
          internalSet(attributes, oldSemconvTableAttribute, sanitizedStatement.getMainIdentifier());
        }
      }
    }

    if (SemconvStability.emitStableDatabaseSemconv()) {
      Long batchSize = getter.getBatchSize(request);
      boolean isBatch = batchSize != null && batchSize > 1;
      if (isBatch) {
        internalSet(attributes, DB_OPERATION_BATCH_SIZE, batchSize);
      }
      if (rawQueryTexts.size() == 1) {
        String rawQueryText = rawQueryTexts.iterator().next();
        SqlStatementInfo sanitizedStatement = sanitizer.sanitize(rawQueryText);
        String operation = sanitizedStatement.getOperation();
        internalSet(
            attributes,
            DB_QUERY_TEXT,
            statementSanitizationEnabled ? sanitizedStatement.getFullStatement() : rawQueryText);
        internalSet(attributes, DB_OPERATION_NAME, isBatch ? "BATCH " + operation : operation);
        if (!SQL_CALL.equals(operation)) {
          internalSet(attributes, DB_COLLECTION_NAME, sanitizedStatement.getMainIdentifier());
        }
      } else {
        MultiQuery multiQuery =
            MultiQuery.analyze(getter.getRawQueryTexts(request), statementSanitizationEnabled);
        internalSet(attributes, DB_QUERY_TEXT, join("; ", multiQuery.getStatements()));

        String operation =
            multiQuery.getOperation() != null ? "BATCH " + multiQuery.getOperation() : "BATCH";
        internalSet(attributes, DB_OPERATION_NAME, operation);

        if (multiQuery.getMainIdentifier() != null
            && (multiQuery.getOperation() == null || !SQL_CALL.equals(multiQuery.getOperation()))) {
          internalSet(attributes, DB_COLLECTION_NAME, multiQuery.getMainIdentifier());
        }
      }
    }
  }

  // String.join is not available on android
  private static String join(String delimiter, Collection<String> collection) {
    StringBuilder builder = new StringBuilder();
    for (String string : collection) {
      if (builder.length() != 0) {
        builder.append(delimiter);
      }
      builder.append(string);
    }
    return builder.toString();
  }
}
