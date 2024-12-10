/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.ktor.v2_0.client

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.util.*
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter
import io.opentelemetry.instrumentation.ktor.v2_0.common.client.AbstractKtorClientTracing
import io.opentelemetry.instrumentation.ktor.v2_0.common.internal.KtorClientTracingUtil

@Deprecated("Use KtorClientTelemetry instead", ReplaceWith("KtorClientTelemetry"))
class KtorClientTracing internal constructor(
  instrumenter: Instrumenter<HttpRequestData, HttpResponse>,
  propagators: ContextPropagators
) : AbstractKtorClientTracing(instrumenter, propagators) {

  companion object : HttpClientPlugin<KtorClientTracingBuilder, KtorClientTracing> {

    override val key = AttributeKey<KtorClientTracing>("OpenTelemetry")

    override fun prepare(block: KtorClientTracingBuilder.() -> Unit) = KtorClientTracingBuilder().apply(block).build()

    override fun install(plugin: KtorClientTracing, scope: HttpClient) {
      KtorClientTracingUtil.install(plugin, scope)
    }
  }
}
