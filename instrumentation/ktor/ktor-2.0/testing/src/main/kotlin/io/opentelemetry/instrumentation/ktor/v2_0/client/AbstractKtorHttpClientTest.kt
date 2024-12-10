/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.ktor.v2_0.client

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.opentelemetry.context.Context
import io.opentelemetry.extension.kotlin.asContextElement
import io.opentelemetry.instrumentation.testing.junit.http.AbstractHttpClientTest
import io.opentelemetry.instrumentation.testing.junit.http.HttpClientResult
import io.opentelemetry.instrumentation.testing.junit.http.HttpClientTestOptions
import io.opentelemetry.instrumentation.testing.junit.http.HttpClientTestOptions.DEFAULT_HTTP_ATTRIBUTES
import io.opentelemetry.semconv.NetworkAttributes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterAll
import java.net.URI

abstract class AbstractKtorHttpClientTest : AbstractHttpClientTest<HttpRequestBuilder>() {

  private val client = HttpClient(CIO) {
    install(HttpRedirect)

    installTracing()
  }
  private val singleConnectionClient = HttpClient(CIO) {
    engine {
      maxConnectionsCount = 1
    }

    installTracing()
  }

  @AfterAll
  fun tearDown() {
    client.close()
    singleConnectionClient.close()
  }

  abstract fun HttpClientConfig<*>.installTracing()

  override fun buildRequest(requestMethod: String, uri: URI, requestHeaders: MutableMap<String, String>) = HttpRequestBuilder(uri.toURL()).apply {
    method = HttpMethod.parse(requestMethod)

    requestHeaders.forEach { (header, value) -> headers.append(header, value) }
  }

  override fun sendRequest(request: HttpRequestBuilder, method: String, uri: URI, headers: MutableMap<String, String>) = runBlocking {
    client.request(request).status.value
  }

  override fun sendRequestWithCallback(
    request: HttpRequestBuilder,
    method: String,
    uri: URI,
    headers: MutableMap<String, String>,
    httpClientResult: HttpClientResult,
  ) {
    CoroutineScope(Dispatchers.Default + Context.current().asContextElement()).launch {
      try {
        val statusCode = client.request(request).status.value
        httpClientResult.complete(statusCode)
      } catch (e: Throwable) {
        httpClientResult.complete(e)
      }
    }
  }

  override fun configure(optionsBuilder: HttpClientTestOptions.Builder) {
    with(optionsBuilder) {
      disableTestReadTimeout()
      markAsLowLevelInstrumentation()
      setMaxRedirects(20)
      spanEndsAfterBody()

      setHttpAttributes { DEFAULT_HTTP_ATTRIBUTES - setOf(NetworkAttributes.NETWORK_PROTOCOL_VERSION) }

      setSingleConnectionFactory { host, port ->
        KtorHttpClientSingleConnection(singleConnectionClient, host, port)
      }
    }
  }
}
