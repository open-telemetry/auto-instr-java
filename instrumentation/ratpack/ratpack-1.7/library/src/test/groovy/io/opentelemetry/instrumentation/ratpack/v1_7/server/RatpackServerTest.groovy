/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.ratpack.v1_7.server

import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.instrumentation.ratpack.v1_7.RatpackServerTelemetry
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.semconv.HttpAttributes
import io.opentelemetry.semconv.UrlAttributes
import ratpack.exec.Blocking
import ratpack.registry.Registry
import ratpack.test.embed.EmbeddedApp
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

class RatpackServerTest extends Specification {

  def spanExporter = InMemorySpanExporter.create()
  def tracerProvider = SdkTracerProvider.builder()
    .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
    .build()

  def openTelemetry = OpenTelemetrySdk.builder()
    .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
    .setTracerProvider(tracerProvider).build()

  def telemetry = RatpackServerTelemetry.create(openTelemetry)

  def cleanup() {
    spanExporter.reset()
  }

  def "add span on handlers"() {
    given:
    def app = EmbeddedApp.of { spec ->
      spec.registry { Registry.of { telemetry.configureRegistry(it) } }
      spec.handlers { chain ->
        chain.get("foo") { ctx -> ctx.render("hi-foo") }
      }
    }

    expect:
    app.test { httpClient ->
      assert "hi-foo" == httpClient.get("foo").body.text

      new PollingConditions().eventually {
        def spanData = spanExporter.finishedSpanItems.find { it.name == "GET /foo" }
        def attributes = spanData.attributes.asMap()

        spanData.kind == SpanKind.SERVER
        attributes[HttpAttributes.HTTP_ROUTE] == "/foo"
        attributes[UrlAttributes.URL_PATH] == "/foo"
        attributes[HttpAttributes.HTTP_REQUEST_METHOD] == "GET"
        attributes[HttpAttributes.HTTP_RESPONSE_STATUS_CODE] == 200L
      }
    }
  }

  def "propagate trace with instrumented async operations"() {
    expect:
    def app = EmbeddedApp.of { spec ->
      spec.registry { Registry.of { telemetry.configureRegistry(it) } }
      spec.handlers { chain ->
        chain.get("foo") { ctx ->
          ctx.render("hi-foo")
          Blocking.op {
            def span = openTelemetry.getTracer("any-tracer").spanBuilder("a-span").startSpan()
            span.makeCurrent().withCloseable {
              span.addEvent("an-event")
              span.end()
            }
          }.then()
        }
      }
    }

    app.test { httpClient ->
      assert "hi-foo" == httpClient.get("foo").body.text

      new PollingConditions().eventually {
        def spanData = spanExporter.finishedSpanItems.find { it.name == "GET /foo" }
        def spanDataChild = spanExporter.finishedSpanItems.find { it.name == "a-span" }

        spanData.kind == SpanKind.SERVER
        spanData.traceId == spanDataChild.traceId
        spanDataChild.parentSpanId == spanData.spanId
        spanDataChild.events.any { it.name == "an-event" }

        def attributes = spanData.attributes.asMap()
        attributes[HttpAttributes.HTTP_ROUTE] == "/foo"
        attributes[UrlAttributes.URL_PATH] == "/foo"
        attributes[HttpAttributes.HTTP_REQUEST_METHOD] == "GET"
        attributes[HttpAttributes.HTTP_RESPONSE_STATUS_CODE] == 200L
      }
    }
  }

  def "propagate trace with instrumented async concurrent operations"() {
    expect:
    def app = EmbeddedApp.of { spec ->
      spec.registry { Registry.of { telemetry.configureRegistry(it) } }
      spec.handlers { chain ->
        chain.get("bar") { ctx ->
          ctx.render("hi-bar")
          Blocking.op {
            def span = openTelemetry.getTracer("any-tracer").spanBuilder("another-span").startSpan()
            span.makeCurrent().withCloseable {
              span.addEvent("an-event")
              span.end()
            }
          }.then()
        }
        chain.get("foo") { ctx ->
          ctx.render("hi-foo")
          Blocking.op {
            def span = openTelemetry.getTracer("any-tracer").spanBuilder("a-span").startSpan()
            span.makeCurrent().withCloseable {
              span.addEvent("an-event")
              span.end()
            }
          }.then()
        }
      }
    }

    app.test { httpClient ->
      assert "hi-foo" == httpClient.get("foo").body.text
      assert "hi-bar" == httpClient.get("bar").body.text

      new PollingConditions().eventually {
        def spanData = spanExporter.finishedSpanItems.find { it.name == "GET /foo" }
        def spanDataChild = spanExporter.finishedSpanItems.find { it.name == "a-span" }

        def spanData2 = spanExporter.finishedSpanItems.find { it.name == "GET /bar" }
        def spanDataChild2 = spanExporter.finishedSpanItems.find { it.name == "another-span" }

        spanData.kind == SpanKind.SERVER
        spanData.traceId == spanDataChild.traceId
        spanDataChild.parentSpanId == spanData.spanId
        spanDataChild.events.any { it.name == "an-event" }

        spanData2.kind == SpanKind.SERVER
        spanData2.traceId == spanDataChild2.traceId
        spanDataChild2.parentSpanId == spanData2.spanId
        spanDataChild2.events.any { it.name == "an-event" }

        def attributes = spanData.attributes.asMap()
        attributes[HttpAttributes.HTTP_ROUTE] == "/foo"
        attributes[UrlAttributes.URL_PATH] == "/foo"
        attributes[HttpAttributes.HTTP_REQUEST_METHOD] == "GET"
        attributes[HttpAttributes.HTTP_RESPONSE_STATUS_CODE] == 200L
      }
    }
  }
}
