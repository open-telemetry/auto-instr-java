/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.ratpack.v1_7.server

import io.opentelemetry.instrumentation.ratpack.server.AbstractRatpackRoutesTest
import io.opentelemetry.instrumentation.ratpack.v1_7.RatpackServerTelemetry
import io.opentelemetry.instrumentation.test.LibraryTestTrait
import ratpack.server.RatpackServerSpec

class RatpackRoutesTest extends AbstractRatpackRoutesTest implements LibraryTestTrait {
  @Override
  void configure(RatpackServerSpec serverSpec) {
    RatpackServerTelemetry telemetry = RatpackServerTelemetry.create(openTelemetry)
    serverSpec.registryOf {
      telemetry.configureRegistry(it)
    }
  }

  @Override
  boolean hasHandlerSpan() {
    return false
  }
}
