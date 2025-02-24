/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.pekkohttp.v1_0

import io.opentelemetry.instrumentation.test.utils.PortUtils
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension
import io.opentelemetry.sdk.testing.assertj.{SpanDataAssert, TraceAssert}
import io.opentelemetry.testing.internal.armeria.client.WebClient
import io.opentelemetry.testing.internal.armeria.common.{
  AggregatedHttpRequest,
  HttpMethod
}
import org.apache.pekko.actor.ActorSystem
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.server.Route
import org.apache.pekko.http.scaladsl.server.Directives.{
  IntNumber,
  complete,
  concat,
  path,
  pathEndOrSingleSlash,
  pathPrefix,
  pathSingleSlash
}
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.api.{AfterAll, Test, TestInstance}

import java.net.{URI, URISyntaxException}
import java.util.function.Consumer
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.DurationInt
import sttp.tapir._
import sttp.tapir.server.pekkohttp.PekkoHttpServerInterpreter

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PekkoHttpServerRouteTest {
  @RegisterExtension private val testing: AgentInstrumentationExtension =
    AgentInstrumentationExtension.create
  private val client: WebClient = WebClient.of()

  implicit val system: ActorSystem = ActorSystem("my-system")

  private def buildAddress(port: Int): URI = try
    new URI("http://localhost:" + port + "/")
  catch {
    case exception: URISyntaxException =>
      throw new IllegalStateException(exception)
  }

  @Test def testSimple(): Unit = {
    val route = path("test") {
      complete("ok")
    }

    test(route, "/test", "GET /test")
  }

  @Test def testRoute(): Unit = {
    val route = concat(
      pathEndOrSingleSlash {
        complete("root")
      },
      pathPrefix("test") {
        concat(
          pathSingleSlash {
            complete("test")
          },
          path(IntNumber) { _ =>
            complete("ok")
          }
        )
      }
    )

    test(route, "/test/1", "GET /test/*")
  }

  @Test def testTapirRoutes(): Unit = {
    val interpreter = PekkoHttpServerInterpreter()(system.dispatcher)
    def makeRoute(input: EndpointInput[Unit]) = {
      interpreter.toRoute(
        endpoint.get
          .in(input)
          .errorOut(stringBody)
          .out(stringBody)
          .serverLogicPure[Future](_ => Right("ok"))
      )
    }

    val route1 = makeRoute("test" / "1")
    val route2 = makeRoute("test" / "2")
    val route3 = makeRoute("test" / "3")
    val route4 = makeRoute("test" / "4")

    val routes1 = concat(route1, route2)
    val routes2 = concat(route3, route4)
    val routes = concat(routes1, routes2)

    test(routes, "/test/4", "GET /test/4")
  }

  def test(route: Route, path: String, spanName: String): Unit = {
    val port = PortUtils.findOpenPort
    val address: URI = buildAddress(port)
    val binding =
      Await.result(Http().bindAndHandle(route, "localhost", port), 10.seconds)
    try {
      val request = AggregatedHttpRequest.of(
        HttpMethod.GET,
        address.resolve(path).toString
      )
      val response = client.execute(request).aggregate.join
      assertThat(response.status.code).isEqualTo(200)
      assertThat(response.contentUtf8).isEqualTo("ok")

      testing.waitAndAssertTraces(new Consumer[TraceAssert] {
        override def accept(trace: TraceAssert): Unit =
          trace.hasSpansSatisfyingExactly(new Consumer[SpanDataAssert] {
            override def accept(span: SpanDataAssert): Unit = {
              span.hasName(spanName)
            }
          })
      })
    } finally {
      binding.unbind()
    }
  }

  @AfterAll
  def cleanUp(): Unit = {
    system.terminate()
  }
}
