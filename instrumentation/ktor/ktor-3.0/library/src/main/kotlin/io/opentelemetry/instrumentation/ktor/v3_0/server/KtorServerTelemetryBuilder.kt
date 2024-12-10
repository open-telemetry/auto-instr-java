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
import io.opentelemetry.instrumentation.ktor.v2_0.common.internal.KtorServerTelemetryUtil
import io.opentelemetry.instrumentation.ktor.v2_0.common.server.AbstractKtorServerTelemetryBuilder
import io.opentelemetry.instrumentation.ktor.v3_0.InstrumentationProperties.INSTRUMENTATION_NAME

class KtorServerTelemetryBuilder internal constructor(
  instrumentationName: String
) : AbstractKtorServerTelemetryBuilder(instrumentationName)

val KtorServerTelemetry = createRouteScopedPlugin("OpenTelemetry", { KtorServerTelemetryBuilder(INSTRUMENTATION_NAME) }) {
  require(pluginConfig.isOpenTelemetryInitialized()) { "OpenTelemetry must be set" }

  KtorServerTelemetryUtil.configureTelemetry(pluginConfig, application)

  application.monitor.subscribe(RoutingRoot.RoutingCallStarted) { call ->
    HttpServerRoute.update(Context.current(), HttpServerRouteSource.SERVER, { _, arg -> arg.route.parent.toString() }, call)
  }
}
