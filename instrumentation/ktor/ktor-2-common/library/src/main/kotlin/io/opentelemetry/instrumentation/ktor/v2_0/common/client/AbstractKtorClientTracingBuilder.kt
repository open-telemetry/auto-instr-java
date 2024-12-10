/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.ktor.v2_0.common.client

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.common.AttributesBuilder
import io.opentelemetry.context.Context
import io.opentelemetry.instrumentation.api.incubator.builder.internal.DefaultHttpClientInstrumenterBuilder
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor
import io.opentelemetry.instrumentation.ktor.v2_0.common.internal.KtorBuilderUtilOld

@Deprecated("Use AbstractKtorClientTelemetryBuilder instead", ReplaceWith("AbstractKtorClientTelemetryBuilder"))
abstract class AbstractKtorClientTracingBuilder(
  private val instrumentationName: String
) {
  companion object {
    init {
      KtorBuilderUtilOld.clientBuilderExtractor = { it.clientBuilder }
    }
  }

  internal lateinit var openTelemetry: OpenTelemetry
  protected lateinit var clientBuilder: DefaultHttpClientInstrumenterBuilder<HttpRequestData, HttpResponse>

  fun setOpenTelemetry(openTelemetry: OpenTelemetry) {
    this.openTelemetry = openTelemetry
    this.clientBuilder = DefaultHttpClientInstrumenterBuilder.create(
      instrumentationName,
      openTelemetry,
      KtorHttpClientAttributesGetter
    )
  }

  protected fun getOpenTelemetry(): OpenTelemetry {
    return openTelemetry
  }

  @Deprecated(
    "Please use method `capturedRequestHeaders`",
    ReplaceWith("capturedRequestHeaders(headers.asIterable())")
  )
  fun setCapturedRequestHeaders(vararg headers: String) = capturedRequestHeaders(headers.asIterable())

  @Deprecated(
    "Please use method `capturedRequestHeaders`",
    ReplaceWith("capturedRequestHeaders(headers)")
  )
  fun setCapturedRequestHeaders(headers: List<String>) = capturedRequestHeaders(headers)

  fun capturedRequestHeaders(vararg headers: String) = capturedRequestHeaders(headers.asIterable())

  fun capturedRequestHeaders(headers: Iterable<String>) {
    clientBuilder.setCapturedRequestHeaders(headers.toList())
  }

  @Deprecated(
    "Please use method `capturedResponseHeaders`",
    ReplaceWith("capturedResponseHeaders(headers.asIterable())")
  )
  fun setCapturedResponseHeaders(vararg headers: String) = capturedResponseHeaders(headers.asIterable())

  @Deprecated(
    "Please use method `capturedResponseHeaders`",
    ReplaceWith("capturedResponseHeaders(headers)")
  )
  fun setCapturedResponseHeaders(headers: List<String>) = capturedResponseHeaders(headers)

  fun capturedResponseHeaders(vararg headers: String) = capturedResponseHeaders(headers.asIterable())

  fun capturedResponseHeaders(headers: Iterable<String>) {
    clientBuilder.setCapturedResponseHeaders(headers.toList())
  }

  @Deprecated(
    "Please use method `knownMethods`",
    ReplaceWith("knownMethods(knownMethods)")
  )
  fun setKnownMethods(knownMethods: Set<String>) = knownMethods(knownMethods)

  fun knownMethods(vararg methods: String) = knownMethods(methods.asIterable())

  fun knownMethods(vararg methods: HttpMethod) = knownMethods(methods.asIterable())

  @JvmName("knownMethodsJvm")
  fun knownMethods(methods: Iterable<HttpMethod>) = knownMethods(methods.map { it.value })

  fun knownMethods(methods: Iterable<String>) {
    clientBuilder.setKnownMethods(methods.toSet())
  }

  @Deprecated("Please use method `attributeExtractor`")
  fun addAttributesExtractors(vararg extractors: AttributesExtractor<in HttpRequestData, in HttpResponse>) = addAttributesExtractors(extractors.asList())

  @Deprecated("Please use method `attributeExtractor`")
  fun addAttributesExtractors(extractors: Iterable<AttributesExtractor<in HttpRequestData, in HttpResponse>>) {
    extractors.forEach {
      attributeExtractor {
        onStart { it.onStart(attributes, parentContext, request) }
        onEnd { it.onEnd(attributes, parentContext, request, response, error) }
      }
    }
  }

  fun attributeExtractor(extractorBuilder: ExtractorBuilder.() -> Unit = {}) {
    val builder = ExtractorBuilder().apply(extractorBuilder).build()
    this.clientBuilder.addAttributesExtractor(
      object : AttributesExtractor<HttpRequestData, HttpResponse> {
        override fun onStart(attributes: AttributesBuilder, parentContext: Context, request: HttpRequestData) {
          builder.onStart(OnStartData(attributes, parentContext, request))
        }

        override fun onEnd(attributes: AttributesBuilder, context: Context, request: HttpRequestData, response: HttpResponse?, error: Throwable?) {
          builder.onEnd(OnEndData(attributes, context, request, response, error))
        }
      }
    )
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

    internal fun build(): Extractor {
      return Extractor(onStart, onEnd)
    }
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

  /**
   * Configures the instrumentation to emit experimental HTTP client metrics.
   *
   * @param emitExperimentalHttpClientMetrics `true` if the experimental HTTP client metrics are to be emitted.
   */
  @Deprecated("Please use method `Experimental.emitExperimentalHttpClientMetrics`")
  fun setEmitExperimentalHttpClientMetrics(emitExperimentalHttpClientMetrics: Boolean) {
    if (emitExperimentalHttpClientMetrics) {
      emitExperimentalHttpClientMetrics()
    }
  }

  @Deprecated("Please use method `Experimental.emitExperimentalHttpClientMetrics`")
  fun emitExperimentalHttpClientMetrics() {
    clientBuilder.setEmitExperimentalHttpClientMetrics(true)
  }
}
