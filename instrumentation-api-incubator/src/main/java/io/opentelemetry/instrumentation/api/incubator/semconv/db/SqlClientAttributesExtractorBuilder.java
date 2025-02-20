/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.incubator.semconv.db;

import static java.util.Objects.requireNonNull;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;

/** A builder of {@link SqlClientAttributesExtractor}. */
public final class SqlClientAttributesExtractorBuilder<REQUEST, RESPONSE> {

  // copied from DbIncubatingAttributes
  private static final AttributeKey<String> DB_SQL_TABLE = AttributeKey.stringKey("db.sql.table");

  final SqlClientAttributesGetter<REQUEST, RESPONSE> getter;
  AttributeKey<String> oldSemconvTableAttribute = DB_SQL_TABLE;
  boolean statementSanitizationEnabled = true;

  SqlClientAttributesExtractorBuilder(SqlClientAttributesGetter<REQUEST, RESPONSE> getter) {
    this.getter = getter;
  }

  /**
   * @deprecated not needed anymore since the new semantic conventions always use db.collection.name
   */
  @CanIgnoreReturnValue
  @Deprecated
  public SqlClientAttributesExtractorBuilder<REQUEST, RESPONSE> setTableAttribute(
      AttributeKey<String> oldSemconvTableAttribute) {
    this.oldSemconvTableAttribute = requireNonNull(oldSemconvTableAttribute);
    return this;
  }

  /**
   * Sets whether the {@code db.statement} attribute extracted by the constructed {@link
   * SqlClientAttributesExtractor} should be sanitized. If set to {@code true}, all parameters that
   * can potentially contain sensitive information will be masked. Enabled by default.
   */
  @CanIgnoreReturnValue
  public SqlClientAttributesExtractorBuilder<REQUEST, RESPONSE> setStatementSanitizationEnabled(
      boolean statementSanitizationEnabled) {
    this.statementSanitizationEnabled = statementSanitizationEnabled;
    return this;
  }

  /**
   * Returns a new {@link SqlClientAttributesExtractor} with the settings of this {@link
   * SqlClientAttributesExtractorBuilder}.
   */
  public AttributesExtractor<REQUEST, RESPONSE> build() {
    return new SqlClientAttributesExtractor<>(
        getter, oldSemconvTableAttribute, statementSanitizationEnabled);
  }
}
