/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.ktor.v2_0

import io.ktor.client.*
import io.opentelemetry.instrumentation.ktor.v2_0.client.KtorClientTracing
import io.opentelemetry.instrumentation.testing.junit.http.HttpClientInstrumentationExtension
import org.junit.jupiter.api.extension.RegisterExtension

class KtorHttpClientOldTest : AbstractKtorHttpClientTest() {

  companion object {
    @JvmStatic
    @RegisterExtension
    private val TESTING = HttpClientInstrumentationExtension.forLibrary()
  }

  override fun HttpClientConfig<*>.installTracing() {
    install(KtorClientTracing) {
      setOpenTelemetry(TESTING.openTelemetry)
      capturedRequestHeaders(TEST_REQUEST_HEADER)
      capturedResponseHeaders(TEST_RESPONSE_HEADER)
    }
  }
}
