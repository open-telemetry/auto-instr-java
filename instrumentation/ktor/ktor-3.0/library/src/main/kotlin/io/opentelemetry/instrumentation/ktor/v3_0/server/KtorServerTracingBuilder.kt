/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.ktor.v3_0.server

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.opentelemetry.context.Context
import io.opentelemetry.instrumentation.api.semconv.http.HttpServerRoute
import io.opentelemetry.instrumentation.api.semconv.http.HttpServerRouteSource
import io.opentelemetry.instrumentation.ktor.v2_0.common.internal.KtorServerTracingUtil
import io.opentelemetry.instrumentation.ktor.v2_0.common.server.AbstractKtorServerTracingBuilder
import io.opentelemetry.instrumentation.ktor.v3_0.InstrumentationProperties.INSTRUMENTATION_NAME

@Deprecated("Use KtorServerTelemetryBuilder instead", ReplaceWith("KtorServerTelemetryBuilder"))
class KtorServerTracingBuilder internal constructor(
  instrumentationName: String
) : AbstractKtorServerTracingBuilder(instrumentationName)

val KtorServerTracing = createRouteScopedPlugin("OpenTelemetry", { KtorServerTracingBuilder(INSTRUMENTATION_NAME) }) {
  require(pluginConfig.isOpenTelemetryInitialized()) { "OpenTelemetry must be set" }

  KtorServerTracingUtil.configureTracing(pluginConfig, application)

  application.monitor.subscribe(RoutingRoot.RoutingCallStarted) { call ->
    HttpServerRoute.update(Context.current(), HttpServerRouteSource.SERVER, { _, arg -> arg.route.parent.toString() }, call)
  }
}
