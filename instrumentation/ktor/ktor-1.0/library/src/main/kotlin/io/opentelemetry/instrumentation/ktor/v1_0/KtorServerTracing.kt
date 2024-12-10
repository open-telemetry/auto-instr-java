/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.ktor.v1_0

import io.ktor.application.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import io.ktor.util.pipeline.*
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.context.Context
import io.opentelemetry.extension.kotlin.asContextElement
import io.opentelemetry.instrumentation.api.incubator.builder.internal.DefaultHttpServerInstrumenterBuilder
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter
import io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor
import io.opentelemetry.instrumentation.api.instrumenter.SpanStatusBuilder
import io.opentelemetry.instrumentation.api.instrumenter.SpanStatusExtractor
import io.opentelemetry.instrumentation.api.internal.InstrumenterUtil
import io.opentelemetry.instrumentation.api.semconv.http.HttpServerRoute
import io.opentelemetry.instrumentation.api.semconv.http.HttpServerRouteSource
import kotlinx.coroutines.withContext

@Deprecated("Use KtorServerTelemetry instead", ReplaceWith("KtorServerTelemetry"))
class KtorServerTracing private constructor(
  private val instrumenter: Instrumenter<ApplicationRequest, ApplicationResponse>,
) {

  class Configuration {
    internal lateinit var builder: DefaultHttpServerInstrumenterBuilder<ApplicationRequest, ApplicationResponse>

    internal var spanKindExtractor:
      (SpanKindExtractor<ApplicationRequest>) -> SpanKindExtractor<ApplicationRequest> = { a -> a }

    fun setOpenTelemetry(openTelemetry: OpenTelemetry) {
      this.builder =
        DefaultHttpServerInstrumenterBuilder.create(
          INSTRUMENTATION_NAME,
          openTelemetry,
          KtorHttpServerAttributesGetter.INSTANCE
        )
    }

    fun setStatusExtractor(
      extractor: (SpanStatusExtractor<in ApplicationRequest, in ApplicationResponse>) -> SpanStatusExtractor<in ApplicationRequest, in ApplicationResponse>
    ) {
      builder.setStatusExtractor { prevExtractor ->
        SpanStatusExtractor { spanStatusBuilder: SpanStatusBuilder,
                              request: ApplicationRequest,
                              response: ApplicationResponse?,
                              throwable: Throwable? ->
          extractor(prevExtractor).extract(spanStatusBuilder, request, response, throwable)
        }
      }
    }

    fun setSpanKindExtractor(extractor: (SpanKindExtractor<ApplicationRequest>) -> SpanKindExtractor<ApplicationRequest>) {
      this.spanKindExtractor = extractor
    }

    fun addAttributeExtractor(extractor: AttributesExtractor<in ApplicationRequest, in ApplicationResponse>) {
      builder.addAttributesExtractor(extractor)
    }

    fun setCapturedRequestHeaders(requestHeaders: List<String>) {
      builder.setCapturedRequestHeaders(requestHeaders)
    }

    fun setCapturedResponseHeaders(responseHeaders: List<String>) {
      builder.setCapturedResponseHeaders(responseHeaders)
    }

    fun setKnownMethods(knownMethods: Set<String>) {
      builder.setKnownMethods(knownMethods)
    }

    internal fun isOpenTelemetryInitialized(): Boolean = this::builder.isInitialized
  }

  private fun start(call: ApplicationCall): Context? {
    val parentContext = Context.current()
    if (!instrumenter.shouldStart(parentContext, call.request)) {
      return null
    }

    return instrumenter.start(parentContext, call.request)
  }

  private fun end(context: Context, call: ApplicationCall, error: Throwable?) {
    instrumenter.end(context, call.request, call.response, error)
  }

  companion object Feature : ApplicationFeature<Application, Configuration, KtorServerTracing> {
    private const val INSTRUMENTATION_NAME = "io.opentelemetry.ktor-1.0"

    private val contextKey = AttributeKey<Context>("OpenTelemetry")
    private val errorKey = AttributeKey<Throwable>("OpenTelemetryException")

    override val key: AttributeKey<KtorServerTracing> = AttributeKey("OpenTelemetry")

    override fun install(pipeline: Application, configure: Configuration.() -> Unit): KtorServerTracing {
      val configuration = Configuration().apply(configure)

      if (!configuration.isOpenTelemetryInitialized()) {
        throw IllegalArgumentException("OpenTelemetry must be set")
      }

      val instrumenter = InstrumenterUtil.buildUpstreamInstrumenter(
        configuration.builder.instrumenterBuilder(),
        ApplicationRequestGetter,
        configuration.spanKindExtractor(SpanKindExtractor.alwaysServer())
      )

      val feature = KtorServerTracing(instrumenter)

      val startPhase = PipelinePhase("OpenTelemetry")
      pipeline.insertPhaseBefore(ApplicationCallPipeline.Monitoring, startPhase)
      pipeline.intercept(startPhase) {
        val context = feature.start(call)

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
      pipeline.sendPipeline.insertPhaseAfter(ApplicationSendPipeline.After, postSendPhase)
      pipeline.sendPipeline.intercept(postSendPhase) {
        val context = call.attributes.getOrNull(contextKey)
        if (context != null) {
          var error: Throwable? = call.attributes.getOrNull(errorKey)
          try {
            proceed()
          } catch (t: Throwable) {
            error = t
            throw t
          } finally {
            feature.end(context, call, error)
          }
        } else {
          proceed()
        }
      }

      pipeline.environment.monitor.subscribe(Routing.RoutingCallStarted) { call ->
        val context = call.attributes.getOrNull(contextKey)
        if (context != null) {
          HttpServerRoute.update(context, HttpServerRouteSource.SERVER, { _, arg -> arg.route.parent.toString() }, call)
        }
      }

      return feature
    }
  }
}
