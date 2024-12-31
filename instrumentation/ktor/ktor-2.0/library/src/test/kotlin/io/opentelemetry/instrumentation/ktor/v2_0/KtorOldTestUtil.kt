/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.ktor.v2_0

import io.ktor.server.application.*
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.instrumentation.ktor.v2_0.server.KtorServerTracing
import io.opentelemetry.instrumentation.testing.junit.http.AbstractHttpServerTest

class KtorOldTestUtil {
  companion object {
    fun installOpenTelemetry(application: Application, openTelemetry: OpenTelemetry) {
      application.install(KtorServerTracing) {
        setOpenTelemetry(openTelemetry)
        capturedRequestHeaders(AbstractHttpServerTest.TEST_REQUEST_HEADER)
        capturedResponseHeaders(AbstractHttpServerTest.TEST_RESPONSE_HEADER)
      }
    }
  }
}
