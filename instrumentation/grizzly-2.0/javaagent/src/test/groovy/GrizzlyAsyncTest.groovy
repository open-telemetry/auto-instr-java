/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

import org.glassfish.grizzly.http.server.HttpServer
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory
import org.glassfish.jersey.server.ResourceConfig

import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.QueryParam
import javax.ws.rs.container.AsyncResponse
import javax.ws.rs.container.Suspended
import javax.ws.rs.core.Response
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.ERROR
import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.EXCEPTION
import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.INDEXED_CHILD
import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.QUERY_PARAM
import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.REDIRECT
import static io.opentelemetry.instrumentation.testing.junit.http.ServerEndpoint.SUCCESS

class GrizzlyAsyncTest extends GrizzlyTest {

  @Override
  HttpServer startServer(int port) {
    ResourceConfig rc = new ResourceConfig()
    rc.register(SimpleExceptionMapper)
    rc.register(AsyncServiceResource)
    GrizzlyHttpServerFactory.createHttpServer(new URI("http://localhost:$port"), rc)
  }

  @Override
  boolean testException() {
    // justification: exception is handled by jersey
    false
  }

  @Override
  boolean testHttpPipelining() {
    false
  }

  @Override
  boolean verifyServerSpanEndTime() {
    // server spans are ended inside of the JAX-RS controller spans
    return false
  }

  @Path("/")
  static class AsyncServiceResource {
    private ExecutorService executor = Executors.newCachedThreadPool()

    @GET
    @Path("success")
    void success(@Suspended AsyncResponse ar) {
      executor.execute {
        controller(SUCCESS) {
          ar.resume(Response.status(SUCCESS.status).entity(SUCCESS.body).build())
        }
      }
    }

    @GET
    @Path("query")
    Response query_param(@QueryParam("some") String param, @Suspended AsyncResponse ar) {
      executor.execute {
        controller(QUERY_PARAM) {
          ar.resume(Response.status(QUERY_PARAM.status).entity("some=$param".toString()).build())
        }
      }
    }

    @GET
    @Path("redirect")
    void redirect(@Suspended AsyncResponse ar) {
      executor.execute {
        controller(REDIRECT) {
          ar.resume(Response.status(REDIRECT.status).location(new URI(REDIRECT.body)).build())
        }
      }
    }

    @GET
    @Path("error-status")
    void error(@Suspended AsyncResponse ar) {
      executor.execute {
        controller(ERROR) {
          ar.resume(Response.status(ERROR.status).entity(ERROR.body).build())
        }
      }
    }

    @GET
    @Path("exception")
    void exception(@Suspended AsyncResponse ar) {
      executor.execute {
        try {
          controller(EXCEPTION) {
            throw new Exception(EXCEPTION.body)
          }
        } catch (Exception e) {
          ar.resume(e)
        }
      }
    }

    @GET
    @Path("child")
    void exception(@QueryParam("id") String id, @Suspended AsyncResponse ar) {
      executor.execute {
        controller(INDEXED_CHILD) {
          INDEXED_CHILD.collectSpanAttributes { it == "id" ? id : null }
          ar.resume(Response.status(INDEXED_CHILD.status).entity(INDEXED_CHILD.body).build())
        }
      }
    }
  }
}
