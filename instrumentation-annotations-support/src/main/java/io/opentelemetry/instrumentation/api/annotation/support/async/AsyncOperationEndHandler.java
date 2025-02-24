/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.annotation.support.async;

import io.opentelemetry.context.Context;
import javax.annotation.Nullable;

/** Callback that is called when async computation completes. */
public interface AsyncOperationEndHandler<REQUEST, RESPONSE> {
  void handle(
      Context context, REQUEST request, @Nullable RESPONSE response, @Nullable Throwable error);
}
