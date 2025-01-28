/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.annotation.support.async;

import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import javax.annotation.Nullable;

/**
 * A wrapper over {@link AsyncOperationEndHandler} that is able to defer {@link
 * AsyncOperationEndHandler#handle(Context, Object, Object, Throwable)} until asynchronous
 * computation finishes.
 */
public final class AsyncOperationEndSupport<REQUEST, RESPONSE> {

  /**
   * Returns a new {@link AsyncOperationEndSupport} that wraps over passed {@code instrumenter},
   * configured for usage with asynchronous computations that are instances of {@code asyncType}. If
   * the result of the async computation ends up being an instance of {@code responseType} it will
   * be passed as the response to the {@code instrumenter} call; otherwise {@code null} value will
   * be used as the response.
   */
  public static <REQUEST, RESPONSE> AsyncOperationEndSupport<REQUEST, RESPONSE> create(
      Instrumenter<REQUEST, RESPONSE> instrumenter,
      Class<RESPONSE> responseType,
      Class<?> asyncType) {
    return new AsyncOperationEndSupport<>(
        instrumenter::end,
        responseType,
        asyncType,
        AsyncOperationEndStrategies.instance().resolveStrategy(asyncType));
  }

  /**
   * Returns a new {@link AsyncOperationEndSupport} that wraps over passed {@code handler},
   * configured for usage with asynchronous computations that are instances of {@code asyncType}. If
   * the result of the async computation ends up being an instance of {@code responseType} it will
   * be passed as the response to the {@code handler} call; otherwise {@code null} value will be
   * used as the response.
   */
  public static <REQUEST, RESPONSE> AsyncOperationEndSupport<REQUEST, RESPONSE> create(
      AsyncOperationEndHandler<REQUEST, RESPONSE> handler,
      Class<RESPONSE> responseType,
      Class<?> asyncType) {
    return new AsyncOperationEndSupport<>(
        handler,
        responseType,
        asyncType,
        AsyncOperationEndStrategies.instance().resolveStrategy(asyncType));
  }

  private final AsyncOperationEndHandler<REQUEST, RESPONSE> handler;
  private final Class<RESPONSE> responseType;
  private final Class<?> asyncType;
  @Nullable private final AsyncOperationEndStrategy asyncOperationEndStrategy;

  private AsyncOperationEndSupport(
      AsyncOperationEndHandler<REQUEST, RESPONSE> handler,
      Class<RESPONSE> responseType,
      Class<?> asyncType,
      @Nullable AsyncOperationEndStrategy asyncOperationEndStrategy) {
    this.handler = handler;
    this.responseType = responseType;
    this.asyncType = asyncType;
    this.asyncOperationEndStrategy = asyncOperationEndStrategy;
  }

  /**
   * Attempts to compose over passed {@code asyncValue} and delay the {@link
   * Instrumenter#end(Context, Object, Object, Throwable)} call until the async operation completes.
   *
   * <p>This method will end the operation immediately if {@code throwable} is passed, if there is
   * no {@link AsyncOperationEndStrategy} for the {@code asyncType} used, or if there is a type
   * mismatch between passed {@code asyncValue} and the {@code asyncType} that was used to create
   * this object.
   *
   * <p>If the passed {@code asyncValue} is recognized as an asynchronous computation, the operation
   * won't be {@link Instrumenter#end(Context, Object, Object, Throwable) ended} until {@code
   * asyncValue} completes.
   */
  @SuppressWarnings("unchecked")
  @Nullable
  public <ASYNC> ASYNC asyncEnd(
      Context context, REQUEST request, @Nullable ASYNC asyncValue, @Nullable Throwable throwable) {
    // we can end early if an exception was thrown
    if (throwable != null) {
      handler.handle(context, request, null, throwable);
      return asyncValue;
    }

    // use the configured strategy to compose over the asyncValue
    if (asyncOperationEndStrategy != null && asyncType.isInstance(asyncValue)) {
      return (ASYNC)
          asyncOperationEndStrategy.end(handler, context, request, asyncValue, responseType);
    }

    // fall back to sync end() if asyncValue type doesn't match
    handler.handle(context, request, tryToGetResponse(responseType, asyncValue), null);
    return asyncValue;
  }

  @Nullable
  public static <RESPONSE> RESPONSE tryToGetResponse(
      Class<RESPONSE> responseType, @Nullable Object asyncValue) {
    if (responseType.isInstance(asyncValue)) {
      return responseType.cast(asyncValue);
    }
    return null;
  }
}
