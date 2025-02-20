/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.ktor.v2_0.common

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributesBuilder
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.context.Context
import io.opentelemetry.instrumentation.api.incubator.builder.internal.DefaultHttpServerInstrumenterBuilder
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor
import io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor
import io.opentelemetry.instrumentation.api.instrumenter.SpanStatusBuilder
import io.opentelemetry.instrumentation.api.instrumenter.SpanStatusExtractor
import io.opentelemetry.instrumentation.ktor.v2_0.common.internal.KtorBuilderUtil
import java.util.function.Function

abstract class AbstractKtorServerTelemetryBuilder(private val instrumentationName: String) {
  companion object {
    init {
      KtorBuilderUtil.serverBuilderExtractor = { it.builder }
    }
  }

  internal lateinit var builder: DefaultHttpServerInstrumenterBuilder<ApplicationRequest, ApplicationResponse>

  internal var spanKindExtractor:
    (SpanKindExtractor<ApplicationRequest>) -> SpanKindExtractor<ApplicationRequest> = { a -> a }

  fun setOpenTelemetry(openTelemetry: OpenTelemetry) {
    this.builder =
      DefaultHttpServerInstrumenterBuilder.create(
        instrumentationName,
        openTelemetry,
        KtorHttpServerAttributesGetter.INSTANCE
      )
  }

  fun spanStatusExtractor(extract: SpanStatusData.(SpanStatusExtractor<ApplicationRequest, ApplicationResponse>) -> Unit) {
    builder.setStatusExtractor { prevExtractor ->
      SpanStatusExtractor {
          spanStatusBuilder: SpanStatusBuilder,
          request: ApplicationRequest,
          response: ApplicationResponse?,
          throwable: Throwable?
        ->
        extract(
          SpanStatusData(spanStatusBuilder, request, response, throwable),
          prevExtractor
        )
      }
    }
  }

  data class SpanStatusData(
    val spanStatusBuilder: SpanStatusBuilder,
    val request: ApplicationRequest,
    val response: ApplicationResponse?,
    val error: Throwable?
  )

  fun spanKindExtractor(extract: ApplicationRequest.(SpanKindExtractor<ApplicationRequest>) -> SpanKind) {
    spanKindExtractor = { prevExtractor ->
      SpanKindExtractor<ApplicationRequest> { request: ApplicationRequest ->
        extract(request, prevExtractor)
      }
    }
  }

  fun attributesExtractor(extractorBuilder: ExtractorBuilder.() -> Unit = {}) {
    val builder = ExtractorBuilder().apply(extractorBuilder).build()
    this.builder.addAttributesExtractor(
      object : AttributesExtractor<ApplicationRequest, ApplicationResponse> {
        override fun onStart(attributes: AttributesBuilder, parentContext: Context, request: ApplicationRequest) {
          builder.onStart(OnStartData(attributes, parentContext, request))
        }

        override fun onEnd(attributes: AttributesBuilder, context: Context, request: ApplicationRequest, response: ApplicationResponse?, error: Throwable?) {
          builder.onEnd(OnEndData(attributes, context, request, response, error))
        }
      }
    )
  }

  fun spanNameExtractor(spanNameExtractorTransformer: Function<SpanNameExtractor<ApplicationRequest>, SpanNameExtractor<ApplicationRequest>>) {
    builder.setSpanNameExtractor(spanNameExtractorTransformer)
  }

  class ExtractorBuilder {
    private var onStart: OnStartData.() -> Unit = {}
    private var onEnd: OnEndData.() -> Unit = {}

    fun onStart(block: OnStartData.() -> Unit) {
      onStart = block
    }

    fun onEnd(block: OnEndData.() -> Unit) {
      onEnd = block
    }

    internal fun build(): Extractor = Extractor(onStart, onEnd)
  }

  internal class Extractor(val onStart: OnStartData.() -> Unit, val onEnd: OnEndData.() -> Unit)

  data class OnStartData(
    val attributes: AttributesBuilder,
    val parentContext: Context,
    val request: ApplicationRequest
  )

  data class OnEndData(
    val attributes: AttributesBuilder,
    val parentContext: Context,
    val request: ApplicationRequest,
    val response: ApplicationResponse?,
    val error: Throwable?
  )

  fun capturedRequestHeaders(vararg headers: String) = capturedRequestHeaders(headers.asIterable())

  fun capturedRequestHeaders(headers: Iterable<String>) {
    builder.setCapturedRequestHeaders(headers.toList())
  }

  fun capturedResponseHeaders(vararg headers: String) = capturedResponseHeaders(headers.asIterable())

  fun capturedResponseHeaders(headers: Iterable<String>) {
    builder.setCapturedResponseHeaders(headers.toList())
  }

  fun knownMethods(vararg methods: String) = knownMethods(methods.asIterable())

  fun knownMethods(vararg methods: HttpMethod) = knownMethods(methods.asIterable())

  @JvmName("knownMethodsJvm")
  fun knownMethods(methods: Iterable<HttpMethod>) = knownMethods(methods.map { it.value })

  fun knownMethods(methods: Iterable<String>) {
    methods.toSet().apply {
      builder.setKnownMethods(this)
    }
  }

  /**
   * {@link #setOpenTelemetry(OpenTelemetry)} sets the serverBuilder to a non-null value.
   */
  fun isOpenTelemetryInitialized(): Boolean = this::builder.isInitialized
}
