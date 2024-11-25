/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.ratpack.v1_7.server

import com.google.inject.AbstractModule
import com.google.inject.Provides
import groovy.transform.CompileStatic
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.ratpack.v1_7.internal.OpenTelemetryServerHandler
import io.opentelemetry.instrumentation.ratpack.v1_7.RatpackFunctionalTest
import io.opentelemetry.instrumentation.ratpack.v1_7.RatpackTelemetry
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.sdk.trace.export.SpanExporter
import ratpack.exec.ExecInitializer
import ratpack.exec.ExecInterceptor
import ratpack.guice.Guice
import ratpack.http.client.HttpClient
import ratpack.server.RatpackServer
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import javax.inject.Singleton

import static io.opentelemetry.semconv.HttpAttributes.HTTP_REQUEST_METHOD
import static io.opentelemetry.semconv.HttpAttributes.HTTP_RESPONSE_STATUS_CODE
import static io.opentelemetry.semconv.HttpAttributes.HTTP_ROUTE
import static io.opentelemetry.semconv.UrlAttributes.URL_PATH

class RatpackServerApplicationTest extends Specification {

  def app = new RatpackFunctionalTest(RatpackApp)

  def "add span on handlers"() {
    expect:
    app.test { httpClient ->
      assert "hi-foo" == httpClient.get("foo").body.text

      new PollingConditions().eventually {
        def spanData = app.spanExporter.finishedSpanItems.find { it.name == "GET /foo" }
        def attributes = spanData.attributes.asMap()

        spanData.kind == SpanKind.SERVER
        attributes[HTTP_ROUTE] == "/foo"
        attributes[URL_PATH] == "/foo"
        attributes[HTTP_REQUEST_METHOD] == "GET"
        attributes[HTTP_RESPONSE_STATUS_CODE] == 200L
      }
    }
  }

  def "propagate trace to http calls"() {
    expect:
    app.test { httpClient ->
      assert "hi-bar" == httpClient.get("bar").body.text

      new PollingConditions().eventually {
        def spanData = app.spanExporter.finishedSpanItems.find { it.name == "GET /bar" }
        def spanDataClient = app.spanExporter.finishedSpanItems.find { it.name == "GET" }
        def attributes = spanData.attributes.asMap()

        spanData.traceId == spanDataClient.traceId

        spanData.kind == SpanKind.SERVER
        attributes[HTTP_ROUTE] == "/bar"
        attributes[URL_PATH] == "/bar"
        attributes[HTTP_REQUEST_METHOD] == "GET"
        attributes[HTTP_RESPONSE_STATUS_CODE] == 200L

        spanDataClient.kind == SpanKind.CLIENT
        def attributesClient = spanDataClient.attributes.asMap()
        attributesClient[HTTP_ROUTE] == "/other"
        attributesClient[HTTP_REQUEST_METHOD] == "GET"
        attributesClient[HTTP_RESPONSE_STATUS_CODE] == 200L
      }
    }
  }

  def "ignore handlers before OpenTelemetryServerHandler"() {
    expect:
    app.test { httpClient ->
      assert "ignored" == httpClient.get("ignore").body.text

      new PollingConditions(initialDelay: 0.1, timeout: 0.3).eventually {
        !app.spanExporter.finishedSpanItems.any { it.name == "GET /ignore" }
      }
    }
  }
}


@CompileStatic
class OpenTelemetryModule extends AbstractModule {
  @Override
  protected void configure() {
    bind(SpanExporter).toInstance(InMemorySpanExporter.create())
  }

  @Singleton
  @Provides
  RatpackTelemetry ratpackTracing(OpenTelemetry openTelemetry) {
    return RatpackTelemetry.create(openTelemetry)
  }

  @Singleton
  @Provides
  OpenTelemetryServerHandler ratpackServerHandler(RatpackTelemetry ratpackTracing) {
    return ratpackTracing.getOpenTelemetryServerHandler()
  }

  @Singleton
  @Provides
  ExecInterceptor ratpackExecInterceptor(RatpackTelemetry ratpackTracing) {
    return ratpackTracing.getOpenTelemetryExecInterceptor()
  }

  @Provides
  @Singleton
  OpenTelemetry providesOpenTelemetry(SpanExporter spanExporter) {
    def tracerProvider = SdkTracerProvider.builder()
      .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
      .build()
    return OpenTelemetrySdk.builder().setTracerProvider(tracerProvider).build()
  }

  @Singleton
  @Provides
  HttpClient instrumentedHttpClient(RatpackTelemetry ratpackTracing) {
    return ratpackTracing.instrumentHttpClient(HttpClient.of {})
  }

  @Singleton
  @Provides
  ExecInitializer ratpackExecInitializer(RatpackTelemetry ratpackTracing) {
    return ratpackTracing.getOpenTelemetryExecInitializer()
  }
}

@CompileStatic
class RatpackApp {

  static void main(String... args) {
    RatpackServer.start { server ->
      server
        .registry(Guice.registry { b -> b.module(OpenTelemetryModule) })
        .handlers { chain ->
          chain
            .get("ignore") { ctx -> ctx.render("ignored") }
            .all(OpenTelemetryServerHandler)
            .get("foo") { ctx -> ctx.render("hi-foo") }
            .get("bar") { ctx ->
              ctx.get(HttpClient).get(ctx.get(URI))
                .then { ctx.render("hi-bar") }
            }
        }
    }
  }
}
