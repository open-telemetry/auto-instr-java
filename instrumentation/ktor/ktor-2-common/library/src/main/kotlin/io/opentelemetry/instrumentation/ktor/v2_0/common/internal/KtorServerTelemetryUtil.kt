/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.ktor.v2_0.common.internal

import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import io.opentelemetry.context.Context
import io.opentelemetry.extension.kotlin.asContextElement
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter
import io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor
import io.opentelemetry.instrumentation.api.internal.InstrumenterUtil
import io.opentelemetry.instrumentation.ktor.v2_0.common.server.AbstractKtorServerTelemetryBuilder
import io.opentelemetry.instrumentation.ktor.v2_0.common.server.ApplicationRequestGetter
import kotlinx.coroutines.withContext

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
object KtorServerTelemetryUtil {

  fun configureTelemetry(builder: AbstractKtorServerTelemetryBuilder, application: Application) {
    val contextKey = AttributeKey<Context>("OpenTelemetry")
    val errorKey = AttributeKey<Throwable>("OpenTelemetryException")

    val instrumenter = instrumenter(builder)
    val tracer = KtorServerTracer(instrumenter)
    val startPhase = PipelinePhase("OpenTelemetry")

    application.insertPhaseBefore(ApplicationCallPipeline.Monitoring, startPhase)
    application.intercept(startPhase) {
      val context = tracer.start(call)

      if (context != null) {
        call.attributes.put(contextKey, context)
        withContext(context.asContextElement()) {
          try {
            proceed()
          } catch (err: Throwable) {
            // Stash error for reporting later since need ktor to finish setting up the response
            call.attributes.put(errorKey, err)
            throw err
          }
        }
      } else {
        proceed()
      }
    }

    val postSendPhase = PipelinePhase("OpenTelemetryPostSend")
    application.sendPipeline.insertPhaseAfter(ApplicationSendPipeline.After, postSendPhase)
    application.sendPipeline.intercept(postSendPhase) {
      val context = call.attributes.getOrNull(contextKey)
      if (context != null) {
        var error: Throwable? = call.attributes.getOrNull(errorKey)
        try {
          proceed()
        } catch (t: Throwable) {
          error = t
          throw t
        } finally {
          tracer.end(context, call, error)
        }
      } else {
        proceed()
      }
    }
  }

  private fun instrumenter(builder: AbstractKtorServerTelemetryBuilder): Instrumenter<ApplicationRequest, ApplicationResponse> {
    return InstrumenterUtil.buildUpstreamInstrumenter(
      builder.builder.instrumenterBuilder(),
      ApplicationRequestGetter,
      builder.spanKindExtractor(SpanKindExtractor.alwaysServer())
    )
  }
}
