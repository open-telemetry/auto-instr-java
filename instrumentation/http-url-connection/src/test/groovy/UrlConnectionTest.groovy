/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import static io.opentelemetry.instrumentation.test.utils.PortUtils.UNUSABLE_PORT
import static io.opentelemetry.instrumentation.test.utils.TraceUtils.runUnderTrace
import static io.opentelemetry.trace.Span.Kind.CLIENT

import io.opentelemetry.instrumentation.test.AgentTestRunner
import io.opentelemetry.instrumentation.api.tracer.HttpClientTracer
import io.opentelemetry.trace.attributes.SemanticAttributes

class UrlConnectionTest extends AgentTestRunner {

  def "trace request with connection failure #scheme"() {
    when:
    runUnderTrace("someTrace") {
      URLConnection connection = url.openConnection()
      connection.setConnectTimeout(10000)
      connection.setReadTimeout(10000)
      assert TEST_TRACER.getCurrentSpan() != null
      connection.inputStream
    }

    then:
    thrown ConnectException

    expect:
    assertTraces(1) {
      trace(0, 2) {
        span(0) {
          name "someTrace"
          hasNoParent()
          errored true
          errorEvent ConnectException, String
        }
        span(1) {
          name expectedOperationName("GET")
          kind CLIENT
          childOf span(0)
          errored true
          errorEvent ConnectException, String
          attributes {
            "${SemanticAttributes.NET_TRANSPORT.key()}" "IP.TCP"
            "${SemanticAttributes.NET_PEER_NAME.key()}" "localhost"
            "${SemanticAttributes.NET_PEER_PORT.key()}" UNUSABLE_PORT
            "${SemanticAttributes.HTTP_URL.key()}" "$url"
            "${SemanticAttributes.HTTP_METHOD.key()}" "GET"
            "${SemanticAttributes.HTTP_FLAVOR.key()}" "1.1"
          }
        }
      }
    }

    where:
    scheme << ["http", "https"]

    url = new URI("$scheme://localhost:$UNUSABLE_PORT").toURL()
  }

  String expectedOperationName(String method) {
    return method != null ? "HTTP $method" : HttpClientTracer.DEFAULT_SPAN_NAME
  }
}
