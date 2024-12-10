/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.ktor.v2_0.common.client

import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter

@Deprecated("Use AbstractKtorClientTelemetry instead", ReplaceWith("AbstractKtorClientTelemetry"))
abstract class AbstractKtorClientTracing(
  private val instrumenter: Instrumenter<HttpRequestData, HttpResponse>,
  private val propagators: ContextPropagators,
) {

  internal fun createSpan(requestBuilder: HttpRequestBuilder): Context? {
    val parentContext = Context.current()
    val requestData = requestBuilder.build()

    return if (instrumenter.shouldStart(parentContext, requestData)) {
      instrumenter.start(parentContext, requestData)
    } else {
      null
    }
  }

  internal fun populateRequestHeaders(requestBuilder: HttpRequestBuilder, context: Context) {
    propagators.textMapPropagator.inject(context, requestBuilder, KtorHttpHeadersSetter)
  }

  internal fun endSpan(context: Context, call: HttpClientCall, error: Throwable?) {
    endSpan(context, HttpRequestBuilder().takeFrom(call.request), call.response, error)
  }

  internal fun endSpan(context: Context, requestBuilder: HttpRequestBuilder, response: HttpResponse?, error: Throwable?) {
    instrumenter.end(context, requestBuilder.build(), response, error)
  }
}
