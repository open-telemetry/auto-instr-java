/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.netty.common.v4_0.internal.client;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public enum NettyConnectionInstrumentationFlag {
  ENABLED,
  ERROR_ONLY,
  DISABLED;

  public static NettyConnectionInstrumentationFlag enabledOrErrorOnly(boolean b) {
    return b ? ENABLED : ERROR_ONLY;
  }
}
