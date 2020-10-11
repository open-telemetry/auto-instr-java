/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package server

import static io.opentelemetry.instrumentation.test.base.HttpServerTest.ServerEndpoint.ERROR
import static io.opentelemetry.instrumentation.test.base.HttpServerTest.ServerEndpoint.EXCEPTION
import static io.opentelemetry.instrumentation.test.base.HttpServerTest.ServerEndpoint.QUERY_PARAM
import static io.opentelemetry.instrumentation.test.base.HttpServerTest.ServerEndpoint.REDIRECT
import static io.opentelemetry.instrumentation.test.base.HttpServerTest.ServerEndpoint.SUCCESS
import static io.opentelemetry.trace.Span.Kind.INTERNAL

import io.opentelemetry.instrumentation.test.asserts.TraceAssert
import io.opentelemetry.instrumentation.test.base.HttpServerTest
import io.opentelemetry.sdk.trace.data.SpanData
import java.util.function.Supplier
import play.BuiltInComponents
import play.Mode
import play.mvc.Results
import play.routing.RoutingDsl
import play.server.Server

class PlayServerTest extends HttpServerTest<Server> {
  @Override
  Server startServer(int port) {
    return Server.forRouter(Mode.TEST, port) { BuiltInComponents components ->
      RoutingDsl.fromComponents(components)
        .GET(SUCCESS.getPath()).routeTo({
        controller(SUCCESS) {
          Results.status(SUCCESS.getStatus(), SUCCESS.getBody())
        }
      } as Supplier)
        .GET(QUERY_PARAM.getPath()).routeTo({
        controller(QUERY_PARAM) {
          Results.status(QUERY_PARAM.getStatus(), QUERY_PARAM.getBody())
        }
      } as Supplier)
        .GET(REDIRECT.getPath()).routeTo({
        controller(REDIRECT) {
          Results.found(REDIRECT.getBody())
        }
      } as Supplier)
        .GET(ERROR.getPath()).routeTo({
        controller(ERROR) {
          Results.status(ERROR.getStatus(), ERROR.getBody())
        }
      } as Supplier)
        .GET(EXCEPTION.getPath()).routeTo({
        controller(EXCEPTION) {
          throw new Exception(EXCEPTION.getBody())
        }
      } as Supplier)
        .build()
    }
  }

  @Override
  void stopServer(Server server) {
    server.stop()
  }

  @Override
  boolean hasHandlerSpan() {
    true
  }

  boolean testExceptionBody() {
    // I can't figure out how to set a proper exception handler to customize the response body.
    false
  }

  @Override
  void handlerSpan(TraceAssert trace, int index, Object parent, String method = "GET", ServerEndpoint endpoint = SUCCESS) {
    trace.span(index) {
      name "play.request"
      kind INTERNAL
      errored endpoint == EXCEPTION
      childOf((SpanData) parent)
      if (endpoint == EXCEPTION) {
        errorEvent(Exception, EXCEPTION.body)
      }
    }
  }

  @Override
  String expectedServerSpanName(String method, ServerEndpoint endpoint) {
    return "akka.request"
  }

}
