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

/**
 * Extractor of <a
 * href="https://github.com/open-telemetry/semantic-conventions/blob/main/docs/database/database-spans.md">database
 * client attributes</a>.
 *
 * <p>This class delegates to a type-specific {@link DbClientAttributesGetter} for individual
 * attribute extraction from request/response objects.
 */
public final class DbClientAttributesExtractor<REQUEST, RESPONSE>
    extends DbClientCommonAttributesExtractor<
        REQUEST, RESPONSE, DbClientAttributesGetter<REQUEST, RESPONSE>> {

  // copied from DbIncubatingAttributes
  private static final AttributeKey<String> DB_STATEMENT = AttributeKey.stringKey("db.statement");
  private static final AttributeKey<String> DB_QUERY_TEXT = AttributeKey.stringKey("db.query.text");
  static final AttributeKey<String> DB_OPERATION = AttributeKey.stringKey("db.operation");
  static final AttributeKey<String> DB_OPERATION_NAME = AttributeKey.stringKey("db.operation.name");
  static final AttributeKey<String> DB_RESPONSE_STATUS_CODE =
      AttributeKey.stringKey("db.response.status_code");

  /** Creates the database client attributes extractor with default configuration. */
  public static <REQUEST, RESPONSE> AttributesExtractor<REQUEST, RESPONSE> create(
      DbClientAttributesGetter<REQUEST, RESPONSE> getter) {
    return new DbClientAttributesExtractor<>(getter);
  }

  DbClientAttributesExtractor(DbClientAttributesGetter<REQUEST, RESPONSE> getter) {
    super(getter);
  }

  @Override
  public void onStart(AttributesBuilder attributes, Context parentContext, REQUEST request) {
    super.onStart(attributes, parentContext, request);

    if (SemconvStability.emitStableDatabaseSemconv()) {
      internalSet(attributes, DB_QUERY_TEXT, getter.getDbQueryText(request));
      internalSet(attributes, DB_OPERATION_NAME, getter.getDbOperationName(request));
    }
    if (SemconvStability.emitOldDatabaseSemconv()) {
      internalSet(attributes, DB_STATEMENT, getter.getDbQueryText(request));
      internalSet(attributes, DB_OPERATION, getter.getDbOperationName(request));
    }
  }
}
