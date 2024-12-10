/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.ratpack.v1_7.server

import io.opentelemetry.instrumentation.ratpack.server.AbstractRatpackForkedHttpServerTest
import io.opentelemetry.instrumentation.ratpack.v1_7.RatpackServerTelemetry
import io.opentelemetry.instrumentation.test.LibraryTestTrait
import io.opentelemetry.instrumentation.testing.junit.http.AbstractHttpServerTest
import io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint
import ratpack.server.RatpackServerSpec

class RatpackForkedHttpServerTest extends AbstractRatpackForkedHttpServerTest implements LibraryTestTrait {
  @Override
  void configure(RatpackServerSpec serverSpec) {
    RatpackServerTelemetry telemetry = RatpackServerTelemetry.builder(openTelemetry)
      .setCapturedRequestHeaders([AbstractHttpServerTest.TEST_REQUEST_HEADER])
      .setCapturedResponseHeaders([AbstractHttpServerTest.TEST_RESPONSE_HEADER])
      .build()
    serverSpec.registryOf {
      telemetry.configureRegistry(it)
    }
  }

  @Override
  boolean hasHandlerSpan(ServerEndpoint endpoint) {
    false
  }
}
