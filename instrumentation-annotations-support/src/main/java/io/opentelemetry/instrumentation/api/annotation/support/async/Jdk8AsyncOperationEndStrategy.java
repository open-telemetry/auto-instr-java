/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.annotation.support.async;

import static io.opentelemetry.instrumentation.api.annotation.support.async.AsyncOperationEndSupport.tryToGetResponse;

import io.opentelemetry.context.Context;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public enum Jdk8AsyncOperationEndStrategy implements AsyncOperationEndStrategy {
  INSTANCE;

  @Override
  public boolean supports(Class<?> asyncType) {
    return asyncType == CompletionStage.class || asyncType == CompletableFuture.class;
  }

  @Override
  public <REQUEST, RESPONSE> Object end(
      AsyncOperationEndHandler<REQUEST, RESPONSE> handler,
      Context context,
      REQUEST request,
      Object asyncValue,
      Class<RESPONSE> responseType) {
    if (asyncValue instanceof CompletableFuture) {
      CompletableFuture<?> future = (CompletableFuture<?>) asyncValue;
      if (tryToEndSynchronously(handler, context, request, future, responseType)) {
        return future;
      }
      return endWhenComplete(handler, context, request, future, responseType);
    }
    CompletionStage<?> stage = (CompletionStage<?>) asyncValue;
    return endWhenComplete(handler, context, request, stage, responseType);
  }

  /**
   * Checks to see if the {@link CompletableFuture} has already been completed and if so
   * synchronously ends the span to avoid additional allocations and overhead registering for
   * notification of completion.
   */
  private static <REQUEST, RESPONSE> boolean tryToEndSynchronously(
      AsyncOperationEndHandler<REQUEST, RESPONSE> handler,
      Context context,
      REQUEST request,
      CompletableFuture<?> future,
      Class<RESPONSE> responseType) {

    if (!future.isDone()) {
      return false;
    }

    try {
      Object potentialResponse = future.join();
      handler.handle(context, request, tryToGetResponse(responseType, potentialResponse), null);
    } catch (Throwable t) {
      handler.handle(context, request, null, t);
    }
    return true;
  }

  /**
   * Registers for notification of the completion of the {@link CompletionStage} at which time the
   * span will be ended.
   */
  private static <REQUEST, RESPONSE> CompletionStage<?> endWhenComplete(
      AsyncOperationEndHandler<REQUEST, RESPONSE> handler,
      Context context,
      REQUEST request,
      CompletionStage<?> stage,
      Class<RESPONSE> responseType) {
    return stage.whenComplete(
        (result, exception) ->
            handler.handle(context, request, tryToGetResponse(responseType, result), exception));
  }
}
