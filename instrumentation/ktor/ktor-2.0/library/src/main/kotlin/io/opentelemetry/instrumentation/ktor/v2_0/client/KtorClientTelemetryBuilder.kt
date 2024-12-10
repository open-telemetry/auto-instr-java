/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.ktor.v2_0.client

import io.opentelemetry.instrumentation.ktor.v2_0.InstrumentationProperties.INSTRUMENTATION_NAME
import io.opentelemetry.instrumentation.ktor.v2_0.common.client.AbstractKtorClientTelemetryBuilder

class KtorClientTelemetryBuilder : AbstractKtorClientTelemetryBuilder(INSTRUMENTATION_NAME) {

  internal fun build(): KtorClientTelemetry = KtorClientTelemetry(
    instrumenter = builder.build(),
    propagators = getOpenTelemetry().propagators,
  )
}
