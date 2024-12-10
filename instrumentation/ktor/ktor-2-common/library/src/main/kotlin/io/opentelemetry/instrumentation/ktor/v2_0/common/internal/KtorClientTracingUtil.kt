/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.ktor.v2_0.common.internal

import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import io.opentelemetry.context.Context
import io.opentelemetry.extension.kotlin.asContextElement
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientRequestResendCount
import io.opentelemetry.instrumentation.ktor.v2_0.common.client.AbstractKtorClientTracing
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
@Deprecated("Use KtorClientTelemetryUtil instead", ReplaceWith("KtorClientTelemetryUtil"))
object KtorClientTracingUtil {
  private val openTelemetryContextKey = AttributeKey<Context>("OpenTelemetry")

  fun install(plugin: AbstractKtorClientTracing, scope: HttpClient) {
    installSpanCreation(plugin, scope)
    installSpanEnd(plugin, scope)
  }

  private fun installSpanCreation(plugin: AbstractKtorClientTracing, scope: HttpClient) {
    val initializeRequestPhase = PipelinePhase("OpenTelemetryInitializeRequest")
    scope.requestPipeline.insertPhaseAfter(HttpRequestPipeline.State, initializeRequestPhase)

    scope.requestPipeline.intercept(initializeRequestPhase) {
      val openTelemetryContext = HttpClientRequestResendCount.initialize(Context.current())
      withContext(openTelemetryContext.asContextElement()) { proceed() }
    }

    val createSpanPhase = PipelinePhase("OpenTelemetryCreateSpan")
    scope.sendPipeline.insertPhaseAfter(HttpSendPipeline.State, createSpanPhase)

    scope.sendPipeline.intercept(createSpanPhase) {
      val requestBuilder = context
      val openTelemetryContext = plugin.createSpan(requestBuilder)

      if (openTelemetryContext != null) {
        try {
          requestBuilder.attributes.put(openTelemetryContextKey, openTelemetryContext)
          plugin.populateRequestHeaders(requestBuilder, openTelemetryContext)

          withContext(openTelemetryContext.asContextElement()) { proceed() }
        } catch (e: Throwable) {
          plugin.endSpan(openTelemetryContext, requestBuilder, null, e)
          throw e
        }
      } else {
        proceed()
      }
    }
  }

  @OptIn(InternalCoroutinesApi::class)
  private fun installSpanEnd(plugin: AbstractKtorClientTracing, scope: HttpClient) {
    val endSpanPhase = PipelinePhase("OpenTelemetryEndSpan")
    scope.receivePipeline.insertPhaseBefore(HttpReceivePipeline.State, endSpanPhase)

    scope.receivePipeline.intercept(endSpanPhase) {
      val openTelemetryContext = it.call.attributes.getOrNull(openTelemetryContextKey)
      openTelemetryContext ?: return@intercept

      scope.launch {
        val job = it.call.coroutineContext.job
        job.join()
        val cause = if (!job.isCancelled) {
          null
        } else {
          kotlin.runCatching { job.getCancellationException() }.getOrNull()
        }

        plugin.endSpan(openTelemetryContext, it.call, cause)
      }
    }
  }
}
