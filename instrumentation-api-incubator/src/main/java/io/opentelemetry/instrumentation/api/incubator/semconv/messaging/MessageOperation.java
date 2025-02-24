/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.incubator.semconv.messaging;

import java.util.Locale;

/**
 * Represents type of <a
 * href="https://github.com/open-telemetry/semantic-conventions/blob/main/docs/messaging/messaging-spans.md#operation-names">operations</a>
 * that may be used in a messaging system.
 */
public enum MessageOperation {
  @Deprecated
  PUBLISH,

  RECEIVE,
  PROCESS,
  CREATE,
  SEND,
  SETTLE;

  /**
   * Returns the operation name as defined in <a
   * href="https://github.com/open-telemetry/semantic-conventions/blob/main/docs/messaging/messaging-spans.md#operation-names">the
   * specification</a>.
   *
   * @deprecated Use {@link #operationType} instead.
   */
  @Deprecated
  String operationName() {
    return name().toLowerCase(Locale.ROOT);
  }

  /**
   * Returns the operation type as defined in <a
   * href="https://github.com/open-telemetry/semantic-conventions/blob/main/docs/messaging/messaging-spans.md#operation-types">the
   * specification</a>.
   */
  String operationType() {
    return name().toLowerCase(Locale.ROOT);
  }
}
