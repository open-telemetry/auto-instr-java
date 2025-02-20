/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.ktor.v2_0.common

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributesBuilder
import io.opentelemetry.context.Context
import io.opentelemetry.instrumentation.api.incubator.builder.internal.DefaultHttpClientInstrumenterBuilder
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor
import io.opentelemetry.instrumentation.ktor.v2_0.common.internal.KtorBuilderUtil
import java.util.function.Function

abstract class AbstractKtorClientTelemetryBuilder(
  private val instrumentationName: String
) {
  companion object {
    init {
      KtorBuilderUtil.clientBuilderExtractor = { it.builder }
    }
  }

  internal lateinit var openTelemetry: OpenTelemetry
  protected lateinit var builder: DefaultHttpClientInstrumenterBuilder<HttpRequestData, HttpResponse>

  internal fun builder(): DefaultHttpClientInstrumenterBuilder<HttpRequestData, HttpResponse> = builder

  fun setOpenTelemetry(openTelemetry: OpenTelemetry) {
    this.openTelemetry = openTelemetry
    this.builder = DefaultHttpClientInstrumenterBuilder.create(
      instrumentationName,
      openTelemetry,
      KtorHttpClientAttributesGetter
    )
  }

  protected fun getOpenTelemetry(): OpenTelemetry = openTelemetry

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
    builder.setKnownMethods(methods.toSet())
  }

  fun attributesExtractor(extractorBuilder: ExtractorBuilder.() -> Unit = {}) {
    val builder = ExtractorBuilder().apply(extractorBuilder).build()
    this.builder.addAttributesExtractor(object : AttributesExtractor<HttpRequestData, HttpResponse> {
      override fun onStart(attributes: AttributesBuilder, parentContext: Context, request: HttpRequestData) {
        builder.onStart(OnStartData(attributes, parentContext, request))
      }

      override fun onEnd(attributes: AttributesBuilder, context: Context, request: HttpRequestData, response: HttpResponse?, error: Throwable?) {
        builder.onEnd(OnEndData(attributes, context, request, response, error))
      }
    })
  }

  fun spanNameExtractor(spanNameExtractorTransformer: Function<SpanNameExtractor<HttpRequestData>, SpanNameExtractor<HttpRequestData>>) {
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
    val request: HttpRequestData
  )

  data class OnEndData(
    val attributes: AttributesBuilder,
    val parentContext: Context,
    val request: HttpRequestData,
    val response: HttpResponse?,
    val error: Throwable?
  )
}
