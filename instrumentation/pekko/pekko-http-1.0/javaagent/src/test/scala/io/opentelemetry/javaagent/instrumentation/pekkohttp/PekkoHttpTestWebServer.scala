/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.pekkohttp

import io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint._
import io.opentelemetry.instrumentation.testing.junit.http.{
  AbstractHttpServerTest,
  ServerEndpoint
}
import org.apache.pekko.http.scaladsl.Http
import org.apache.pekko.http.scaladsl.Http.ServerBinding
import org.apache.pekko.http.scaladsl.model._
import org.apache.pekko.http.scaladsl.server.Directives._
import org.apache.pekko.http.scaladsl.server.ExceptionHandler

import java.util.function.Supplier
import scala.concurrent.Await

object PekkoHttpTestWebServer extends PekkoImplicits {

  val exceptionHandler = ExceptionHandler { case ex: Exception =>
    complete(
      HttpResponse(status = EXCEPTION.getStatus).withEntity(ex.getMessage)
    )
  }

  val route = handleExceptions(exceptionHandler) {
    extractUri { uri =>
      val endpoint = ServerEndpoint.forPath(uri.path.toString())
      complete {
        AbstractHttpServerTest.controller(
          endpoint,
          new Supplier[HttpResponse] {
            def get(): HttpResponse = {
              val resp = HttpResponse(status = endpoint.getStatus)
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
  }

  private var binding: ServerBinding = null

  def start(port: Int): Unit = synchronized {
    if (null == binding) {
      import scala.concurrent.duration._
      binding = Await.result(
        Http().newServerAt("localhost", port).bindFlow(route),
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
