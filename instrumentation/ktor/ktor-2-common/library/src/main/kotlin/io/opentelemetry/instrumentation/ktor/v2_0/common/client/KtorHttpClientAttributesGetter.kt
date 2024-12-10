/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.ktor.v2_0.common.client

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientAttributesGetter

internal object KtorHttpClientAttributesGetter : HttpClientAttributesGetter<HttpRequestData, HttpResponse> {

  override fun getUrlFull(request: HttpRequestData) = request.url.toString()

  override fun getHttpRequestMethod(request: HttpRequestData) = request.method.value

  override fun getHttpRequestHeader(request: HttpRequestData, name: String) = request.headers.getAll(name).orEmpty()

  override fun getHttpResponseStatusCode(request: HttpRequestData, response: HttpResponse, error: Throwable?) = response.status.value

  override fun getHttpResponseHeader(request: HttpRequestData, response: HttpResponse, name: String) = response.headers.getAll(name).orEmpty()

  override fun getNetworkProtocolName(request: HttpRequestData, response: HttpResponse?): String? = response?.version?.name

  override fun getNetworkProtocolVersion(request: HttpRequestData, response: HttpResponse?): String? {
    val version = response?.version ?: return null
    if (version.minor == 0) {
      return "${version.major}"
    }
    return "${version.major}.${version.minor}"
  }

  override fun getServerAddress(request: HttpRequestData) = request.url.host

  override fun getServerPort(request: HttpRequestData) = request.url.port
}
