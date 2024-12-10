/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.ktor.v2_0.common.internal

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.opentelemetry.instrumentation.api.incubator.builder.internal.DefaultHttpClientInstrumenterBuilder
import io.opentelemetry.instrumentation.api.incubator.builder.internal.DefaultHttpServerInstrumenterBuilder
import io.opentelemetry.instrumentation.ktor.v2_0.common.client.AbstractKtorClientTracingBuilder
import io.opentelemetry.instrumentation.ktor.v2_0.common.server.AbstractKtorServerTracingBuilder

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
@Deprecated("Use KtorBuilderUtil instead", ReplaceWith("KtorBuilderUtil"))
object KtorBuilderUtilOld {
  lateinit var clientBuilderExtractor: (AbstractKtorClientTracingBuilder) -> DefaultHttpClientInstrumenterBuilder<HttpRequestData, HttpResponse>
  lateinit var serverBuilderExtractor: (
    AbstractKtorServerTracingBuilder
  ) -> DefaultHttpServerInstrumenterBuilder<ApplicationRequest, ApplicationResponse>
}
