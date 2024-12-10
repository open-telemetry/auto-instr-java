/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.ktor.v1_0

import io.ktor.application.*
import io.ktor.http.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension
import io.opentelemetry.instrumentation.testing.junit.http.AbstractHttpServerUsingTest
import io.opentelemetry.instrumentation.testing.junit.http.HttpServerInstrumentationExtension
import io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint
import io.opentelemetry.testing.internal.armeria.common.AggregatedHttpRequest
import io.opentelemetry.testing.internal.armeria.common.AggregatedHttpResponse
import io.opentelemetry.testing.internal.armeria.common.HttpMethod
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import java.util.stream.Stream

class KtorServerSpanKindExtractorTest : AbstractHttpServerUsingTest<ApplicationEngine>() {

  private val consumerKindEndpoint = ServerEndpoint("consumerKindEndpoint", "from-pubsub/run", 200, "")
  private val serverKindEndpoint = ServerEndpoint("serverKindEndpoint", "from-client/run", 200, "")

  companion object {
    @JvmStatic
    @RegisterExtension
    val testing: InstrumentationExtension = HttpServerInstrumentationExtension.forLibrary()
  }

  @BeforeAll
  fun setupOptions() {
    startServer()
  }

  @AfterAll
  fun cleanup() {
    cleanupServer()
  }

  override fun getContextPath() = ""

  override fun setupServer(): ApplicationEngine {
    return embeddedServer(Netty, port = port) {
      install(KtorServerTelemetry) {
        setOpenTelemetry(testing.openTelemetry)
        setSpanKindExtractor {
          SpanKindExtractor { req ->
            if (req.uri.startsWith("/from-pubsub/")) {
              SpanKind.CONSUMER
            } else {
              SpanKind.SERVER
            }
          }
        }
      }

      routing {
        post(consumerKindEndpoint.path) {
          call.respondText(consumerKindEndpoint.body, status = HttpStatusCode.fromValue(consumerKindEndpoint.status))
        }

        post(serverKindEndpoint.path) {
          call.respondText(serverKindEndpoint.body, status = HttpStatusCode.fromValue(serverKindEndpoint.status))
        }
      }
    }.start()
  }

  override fun stopServer(server: ApplicationEngine) {
    server.stop(0, 10, TimeUnit.SECONDS)
  }

  @ParameterizedTest
  @MethodSource("provideArguments")
  fun testSpanKindExtractor(endpoint: ServerEndpoint, expectedKind: SpanKind) {
    val request = AggregatedHttpRequest.of(HttpMethod.valueOf("POST"), resolveAddress(endpoint))
    val response: AggregatedHttpResponse = client.execute(request).aggregate().join()
    assertThat(response.status().code()).isEqualTo(endpoint.status)

    testing.waitAndAssertTraces(
      Consumer { trace ->
        trace.hasSpansSatisfyingExactly(
          Consumer { span ->
            span.hasKind(expectedKind)
          }
        )
      }
    )
  }

  private fun provideArguments(): Stream<Arguments> {
    return Stream.of(
      arguments(consumerKindEndpoint, SpanKind.CONSUMER),
      arguments(serverKindEndpoint, SpanKind.SERVER),
    )
  }
}
