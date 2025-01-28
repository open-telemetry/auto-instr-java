/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.annotation.support.async;

import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;

/**
 * Implementations of this interface describe how to compose over {@linkplain #supports(Class)
 * supported} asynchronous computation types and delay marking the operation as ended by calling
 * {@link Instrumenter#end(Context, Object, Object, Throwable)} or {@link
 * AsyncOperationEndHandler#handle(Context, Object, Object, Throwable)}.
 */
public interface AsyncOperationEndStrategy {

  /**
   * Returns true for every asynchronous computation type {@code asyncType} this strategy supports.
   */
  boolean supports(Class<?> asyncType);

  /**
   * Composes over {@code asyncValue} and delays the {@link Instrumenter#end(Context, Object,
   * Object, Throwable)} call until after the asynchronous operation represented by {@code
   * asyncValue} completes.
   *
   * @param instrumenter The {@link Instrumenter} to be used to end the operation stored in the
   *     {@code context}.
   * @param asyncValue Return value from the instrumented method. Must be an instance of a {@code
   *     asyncType} for which {@link #supports(Class)} returned true (in particular it must not be
   *     {@code null}).
   * @param responseType Expected type of the response that should be obtained from the {@code
   *     asyncValue}. If the result of the async computation is instance of the passed type it will
   *     be passed when the {@code instrumenter} is called.
   * @return Either {@code asyncValue} or a value composing over {@code asyncValue} for notification
   *     of completion.
   */
  default <REQUEST, RESPONSE> Object end(
      Instrumenter<REQUEST, RESPONSE> instrumenter,
      Context context,
      REQUEST request,
      Object asyncValue,
      Class<RESPONSE> responseType) {
    return end(instrumenter::end, context, request, asyncValue, responseType);
  }

  /**
   * Composes over {@code asyncValue} and delays the {@link AsyncOperationEndHandler#handle(Context,
   * Object, Object, Throwable)} call until after the asynchronous operation represented by {@code
   * asyncValue} completes.
   *
   * @param handler The {@link AsyncOperationEndHandler} to be used to end the operation stored in
   *     the {@code context}.
   * @param asyncValue Return value from the instrumented method. Must be an instance of a {@code
   *     asyncType} for which {@link #supports(Class)} returned true (in particular it must not be
   *     {@code null}).
   * @param responseType Expected type of the response that should be obtained from the {@code
   *     asyncValue}. If the result of the async computation is instance of the passed type it will
   *     be passed when the {@code handler} is called.
   * @return Either {@code asyncValue} or a value composing over {@code asyncValue} for notification
   *     of completion.
   */
  <REQUEST, RESPONSE> Object end(
      AsyncOperationEndHandler<REQUEST, RESPONSE> handler,
      Context context,
      REQUEST request,
      Object asyncValue,
      Class<RESPONSE> responseType);
}
