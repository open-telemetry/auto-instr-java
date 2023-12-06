/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.incubator.semconv.db;

import static io.opentelemetry.instrumentation.api.internal.AttributesExtractorUtil.internalSet;

import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.semconv.SemanticAttributes;

/**
 * Extractor of <a
 * href="https://github.com/open-telemetry/semantic-conventions/blob/main/docs/database/database-spans.md">database
 * client attributes</a>.
 *
 * <p>This class delegates to a type-specific {@link DbClientAttributeGetter} for individual
 * attribute extraction from request/response objects.
 */
public final class DbClientAttributesExtractor<REQUEST, RESPONSE>
    extends DbClientCommonAttributesExtractor<REQUEST, RESPONSE, DbClientAttributeGetter<REQUEST>> {

  /** Creates the database client attributes extractor with default configuration. */
  public static <REQUEST, RESPONSE> AttributesExtractor<REQUEST, RESPONSE> create(
      DbClientAttributeGetter<REQUEST> getter) {
    return new DbClientAttributesExtractor<>(getter);
  }

  DbClientAttributesExtractor(DbClientAttributeGetter<REQUEST> getter) {
    super(getter);
  }

  @Override
  public void onStart(AttributesBuilder attributes, Context parentContext, REQUEST request) {
    super.onStart(attributes, parentContext, request);

    internalSet(attributes, SemanticAttributes.DB_STATEMENT, getter.getStatement(request));
    internalSet(attributes, SemanticAttributes.DB_OPERATION, getter.getOperation(request));
  }
}
