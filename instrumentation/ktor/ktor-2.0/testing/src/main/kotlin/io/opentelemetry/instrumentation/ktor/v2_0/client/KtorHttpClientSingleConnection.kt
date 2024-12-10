/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.ktor.v2_0.client

import io.ktor.client.*
import io.ktor.client.request.*
import io.opentelemetry.instrumentation.testing.junit.http.SingleConnection
import kotlinx.coroutines.runBlocking

class KtorHttpClientSingleConnection(
  private val client: HttpClient,
  private val host: String,
  private val port: Int
) : SingleConnection {

  override fun doRequest(path: String, requestHeaders: MutableMap<String, String>) = runBlocking {
    val request = HttpRequestBuilder(
      scheme = "http",
      host = host,
      port = port,
      path = path,
    ).apply {
      requestHeaders.forEach { (name, value) -> headers.append(name, value) }
    }

    client.request(request).status.value
  }
}
