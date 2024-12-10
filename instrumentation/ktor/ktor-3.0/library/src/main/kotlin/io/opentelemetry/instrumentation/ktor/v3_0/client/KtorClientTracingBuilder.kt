/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.ktor.v3_0.client

import io.opentelemetry.instrumentation.ktor.v2_0.common.client.AbstractKtorClientTracingBuilder
import io.opentelemetry.instrumentation.ktor.v3_0.InstrumentationProperties.INSTRUMENTATION_NAME

@Deprecated("Use KtorClientTelemetryBuilder instead", ReplaceWith("KtorClientTelemetryBuilder"))
class KtorClientTracingBuilder : AbstractKtorClientTracingBuilder(INSTRUMENTATION_NAME) {

  internal fun build(): KtorClientTracing = KtorClientTracing(
    instrumenter = clientBuilder.build(),
    propagators = getOpenTelemetry().propagators,
  )
}
