/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.ktor.v3_0.client

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.util.*
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter
import io.opentelemetry.instrumentation.ktor.v2_0.common.client.AbstractKtorClientTelemetry
import io.opentelemetry.instrumentation.ktor.v2_0.common.internal.KtorClientTelemetryUtil

class KtorClientTelemetry internal constructor(
  instrumenter: Instrumenter<HttpRequestData, HttpResponse>,
  propagators: ContextPropagators
) : AbstractKtorClientTelemetry(instrumenter, propagators) {

  companion object : HttpClientPlugin<KtorClientTelemetryBuilder, KtorClientTelemetry> {

    override val key = AttributeKey<KtorClientTelemetry>("OpenTelemetry")

    override fun prepare(block: KtorClientTelemetryBuilder.() -> Unit) = KtorClientTelemetryBuilder().apply(block).build()

    override fun install(plugin: KtorClientTelemetry, scope: HttpClient) {
      KtorClientTelemetryUtil.install(plugin, scope)
    }
  }
}
