/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.pekkohttp

import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.Http.ServerBinding
import org.apache.pekko.http.scaladsl.model.HttpMethods.GET
import org.apache.pekko.http.scaladsl.model._
import io.opentelemetry.instrumentation.testing.junit.http.{
  AbstractHttpServerTest,
  ServerEndpoint
}
import io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint._

import java.util.function.Supplier
import scala.concurrent.{Await, Future}

object PekkoHttpTestAsyncWebServer extends PekkoImplicits {

  val asyncHandler: HttpRequest => Future[HttpResponse] = {
    case HttpRequest(GET, uri: Uri, _, _, _) =>
      Future {
        val endpoint = ServerEndpoint.forPath(uri.path.toString())
        AbstractHttpServerTest.controller(
          endpoint,
          new Supplier[HttpResponse] {
            def get(): HttpResponse = {
              val resp = HttpResponse(status =
                endpoint.getStatus
              ) // .withHeaders(headers.Type)resp.contentType = "text/plain"
              endpoint match {
                case SUCCESS => resp.withEntity(endpoint.getBody)
                case INDEXED_CHILD =>
                  INDEXED_CHILD.collectSpanAttributes(new UrlParameterProvider {
                    override def getParameter(name: String): String =
                      uri.query().get(name).orNull
                  })
                  resp.withEntity("")
                case QUERY_PARAM => resp.withEntity(uri.queryString().orNull)
                case REDIRECT =>
                  resp.withHeaders(headers.Location(endpoint.getBody))
                case ERROR     => resp.withEntity(endpoint.getBody)
                case EXCEPTION => throw new Exception(endpoint.getBody)
                case _ =>
                  HttpResponse(status = NOT_FOUND.getStatus)
                    .withEntity(NOT_FOUND.getBody)
              }
            }
          }
        )
      }
  }

  private var binding: ServerBinding = _

  def start(port: Int): Unit = synchronized {
    if (null == binding) {
      import scala.concurrent.duration._
      binding = Await.result(
        Http().newServerAt("localhost", port).bind(asyncHandler),
        10.seconds
      )
    }
  }

  def stop(): Unit = synchronized {
    if (null != binding) {
      binding.unbind()
      system.terminate()
      binding = null
    }
  }
}
