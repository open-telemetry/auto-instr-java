/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal;

import io.opentelemetry.api.OpenTelemetry;
import java.io.Serializable;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * Wrapper for OpenTelemetry that can be injected into kafka configuration without breaking
 * serialization. https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/7597
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
public final class OpenTelemetrySupplier implements Supplier<OpenTelemetry>, Serializable {
  private static final long serialVersionUID = 1L;
  private final transient OpenTelemetry openTelemetry;

  public OpenTelemetrySupplier(OpenTelemetry openTelemetry) {
    Objects.requireNonNull(openTelemetry);
    this.openTelemetry = openTelemetry;
  }

  @Override
  public OpenTelemetry get() {
    return openTelemetry;
  }

  private Object writeReplace() {
    // serialize this object to null
    return null;
  }
}
