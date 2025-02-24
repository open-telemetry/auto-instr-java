/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.netty.common.v4_0.internal.client;

import io.opentelemetry.context.Context;
import javax.annotation.Nullable;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public interface NettySslInstrumenter {

  boolean shouldStart(Context parentContext, NettySslRequest request);

  Context start(Context parentContext, NettySslRequest request);

  void end(Context context, NettySslRequest request, @Nullable Throwable error);
}
