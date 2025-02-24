/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.kotlinxcoroutines.flow;

import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.annotation.support.async.AsyncOperationEndHandler;
import io.opentelemetry.instrumentation.api.annotation.support.async.AsyncOperationEndStrategies;
import io.opentelemetry.instrumentation.api.annotation.support.async.AsyncOperationEndStrategy;
import kotlinx.coroutines.flow.Flow;

public final class FlowInstrumentationHelper {
  private static final FlowAsyncOperationEndStrategy asyncOperationEndStrategy =
      new FlowAsyncOperationEndStrategy();

  static {
    AsyncOperationEndStrategies.instance().registerStrategy(asyncOperationEndStrategy);
  }

  public static void initialize() {}

  private FlowInstrumentationHelper() {}

  private static final class FlowAsyncOperationEndStrategy implements AsyncOperationEndStrategy {

    @Override
    public boolean supports(Class<?> returnType) {
      return Flow.class.isAssignableFrom(returnType);
    }

    @Override
    public <REQUEST, RESPONSE> Object end(
        AsyncOperationEndHandler<REQUEST, RESPONSE> handler,
        Context context,
        REQUEST request,
        Object asyncValue,
        Class<RESPONSE> responseType) {
      Flow<?> flow = (Flow<?>) asyncValue;
      return FlowUtilKt.onComplete(flow, handler, context, request);
    }
  }
}
