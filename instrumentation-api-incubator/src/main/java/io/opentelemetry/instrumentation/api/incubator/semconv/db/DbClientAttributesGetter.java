/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.incubator.semconv.db;

import javax.annotation.Nullable;

/**
 * An interface for getting database client attributes.
 *
 * <p>Instrumentation authors will create implementations of this interface for their specific
 * library/framework. It will be used by the {@link DbClientAttributesExtractor} to obtain the
 * various database client attributes in a type-generic way.
 *
 * <p>If an attribute is not available in this library, it is appropriate to return {@code null}
 * from the attribute methods, but implement as many as possible for best compliance with the
 * OpenTelemetry specification.
 */
public interface DbClientAttributesGetter<REQUEST, RESPONSE>
    extends DbClientCommonAttributesGetter<REQUEST, RESPONSE> {

  /**
   * @deprecated Use {@link #getDbQueryText(REQUEST)} instead.
   */
  @Deprecated
  @Nullable
  default String getStatement(REQUEST request) {
    return null;
  }

  // TODO: make this required to implement
  @Nullable
  default String getDbQueryText(REQUEST request) {
    return getStatement(request);
  }

  /**
   * @deprecated Use {@link #getDbOperationName(REQUEST)} instead.
   */
  @Deprecated
  @Nullable
  default String getOperation(REQUEST request) {
    return null;
  }

  // TODO: make this required to implement
  @Nullable
  default String getDbOperationName(REQUEST request) {
    return getOperation(request);
  }
}
