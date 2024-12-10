/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.ratpack.v1_7.client

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.api.trace.Tracer
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator
import io.opentelemetry.context.Context
import io.opentelemetry.context.propagation.ContextPropagators
import io.opentelemetry.instrumentation.ratpack.v1_7.RatpackClientTelemetry
import io.opentelemetry.instrumentation.ratpack.v1_7.RatpackServerTelemetry
import io.opentelemetry.sdk.OpenTelemetrySdk
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter
import io.opentelemetry.sdk.trace.SdkTracerProvider
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor
import io.opentelemetry.semconv.UrlAttributes
import ratpack.exec.Execution
import ratpack.exec.Promise
import ratpack.func.Action
import ratpack.guice.Guice
import ratpack.http.client.HttpClient
import ratpack.service.Service
import ratpack.service.StartEvent
import ratpack.test.embed.EmbeddedApp
import spock.lang.Specification
import spock.util.concurrent.PollingConditions

import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

import static io.opentelemetry.api.trace.SpanKind.CLIENT
import static io.opentelemetry.api.trace.SpanKind.SERVER
import static io.opentelemetry.semconv.HttpAttributes.*

class InstrumentedHttpClientTest extends Specification {

  def spanExporter = InMemorySpanExporter.create()
  def tracerProvider = SdkTracerProvider.builder()
    .addSpanProcessor(SimpleSpanProcessor.create(spanExporter))
    .build()

  def openTelemetry = OpenTelemetrySdk.builder()
    .setPropagators(ContextPropagators.create(W3CTraceContextPropagator.getInstance()))
    .setTracerProvider(tracerProvider).build()

  RatpackClientTelemetry telemetry = RatpackClientTelemetry.create(openTelemetry)
  RatpackServerTelemetry serverTelemetry = RatpackServerTelemetry.create(openTelemetry)

  def cleanup() {
    spanExporter.reset()
  }

  def "propagate trace with http calls"() {
    expect:
    def otherApp = EmbeddedApp.of { spec ->
      spec.registry(
        Guice.registry { bindings ->
          serverTelemetry.configureRegistry(bindings)
          telemetry.configureRegistry(bindings)
        }
      )
      spec.handlers {
        it.get("bar") { ctx -> ctx.render("foo") }
      }
    }

    def app = EmbeddedApp.of { spec ->
      spec.registry(
        Guice.registry { bindings ->
          serverTelemetry.configureRegistry(bindings)
          telemetry.configureRegistry(bindings)
          bindings.bindInstance(HttpClient, telemetry.instrument(HttpClient.of(Action.noop())))
        }
      )

      spec.handlers { chain ->
        chain.get("foo") { ctx ->
          HttpClient instrumentedHttpClient = ctx.get(HttpClient)
          instrumentedHttpClient.get(new URI("${otherApp.address}bar"))
            .then { ctx.render("bar") }
        }
      }
    }

    app.test { httpClient ->
      assert "bar" == httpClient.get("foo").body.text

      new PollingConditions().eventually {
        def spanData = spanExporter.finishedSpanItems.find { it.name == "GET /foo" }
        def spanClientData = spanExporter.finishedSpanItems.find { it.name == "GET" && it.kind == CLIENT }
        def spanDataApi = spanExporter.finishedSpanItems.find { it.name == "GET /bar" && it.kind == SERVER }

        spanData.traceId == spanClientData.traceId
        spanData.traceId == spanDataApi.traceId

        spanData.kind == SERVER
        spanClientData.kind == CLIENT
        def atts = spanClientData.attributes.asMap()
        atts[HTTP_ROUTE] == "/bar"
        atts[HTTP_REQUEST_METHOD] == "GET"
        atts[HTTP_RESPONSE_STATUS_CODE] == 200L

        def attributes = spanData.attributes.asMap()
        attributes[HTTP_ROUTE] == "/foo"
        attributes[UrlAttributes.URL_PATH] == "/foo"
        attributes[HTTP_REQUEST_METHOD] == "GET"
        attributes[HTTP_RESPONSE_STATUS_CODE] == 200L

        def attsApi = spanDataApi.attributes.asMap()
        attsApi[HTTP_ROUTE] == "/bar"
        attsApi[UrlAttributes.URL_PATH] == "/bar"
        attsApi[HTTP_REQUEST_METHOD] == "GET"
        attsApi[HTTP_RESPONSE_STATUS_CODE] == 200L
      }
    }
  }

  def "add spans for multiple concurrent client calls"() {
    expect:
    def latch = new CountDownLatch(2)

    def otherApp = EmbeddedApp.of { spec ->
      spec.handlers { chain ->
        chain.get("foo") { ctx -> ctx.render("bar") }
        chain.get("bar") { ctx -> ctx.render("foo") }
      }
    }

    def app = EmbeddedApp.of { spec ->
      spec.registry(
        Guice.registry { bindings ->
          serverTelemetry.configureRegistry(bindings)
          telemetry.configureRegistry(bindings)
          bindings.bindInstance(HttpClient, telemetry.instrument(HttpClient.of(Action.noop())))
        }
      )

      spec.handlers { chain ->
        chain.get("path-name") { ctx ->
          ctx.render("hello")
          def instrumentedHttpClient = ctx.get(HttpClient)
          instrumentedHttpClient.get(new URI("${otherApp.address}foo")).then { latch.countDown() }
          instrumentedHttpClient.get(new URI("${otherApp.address}bar")).then { latch.countDown() }
        }
      }
    }

    app.test { httpClient ->
      assert "hello" == httpClient.get("path-name").body.text
      latch.await(1, TimeUnit.SECONDS)

      new PollingConditions().eventually {
        spanExporter.finishedSpanItems.size() == 3
        def spanData = spanExporter.finishedSpanItems.find { spanData -> spanData.name == "GET /path-name" }
        def spanClientData1 = spanExporter.finishedSpanItems.find { s -> s.name == "GET" && s.attributes.asMap()[HTTP_ROUTE] == "/foo" }
        def spanClientData2 = spanExporter.finishedSpanItems.find { s -> s.name == "GET" && s.attributes.asMap()[HTTP_ROUTE] == "/bar" }

        spanData.traceId == spanClientData1.traceId
        spanData.traceId == spanClientData2.traceId

        spanData.kind == SERVER

        spanClientData1.kind == CLIENT
        def atts = spanClientData1.attributes.asMap()
        atts[HTTP_ROUTE] == "/foo"
        atts[HTTP_REQUEST_METHOD] == "GET"
        atts[HTTP_RESPONSE_STATUS_CODE] == 200L

        spanClientData2.kind == CLIENT
        def atts2 = spanClientData2.attributes.asMap()
        atts2[HTTP_ROUTE] == "/bar"
        atts2[HTTP_REQUEST_METHOD] == "GET"
        atts2[HTTP_RESPONSE_STATUS_CODE] == 200L

        def attributes = spanData.attributes.asMap()
        attributes[HTTP_ROUTE] == "/path-name"
        attributes[UrlAttributes.URL_PATH] == "/path-name"
        attributes[HTTP_REQUEST_METHOD] == "GET"
        attributes[HTTP_RESPONSE_STATUS_CODE] == 200L
      }
    }
  }

  def "handling exception errors in http client"() {
    expect:
    def otherApp = EmbeddedApp.of { spec ->
      spec.handlers {
        it.get("foo") { ctx ->
          Promise.value("bar").defer(Duration.ofSeconds(1L))
            .then { ctx.render("bar") }
        }
      }
    }

    def app = EmbeddedApp.of { spec ->
      spec.registry(
        Guice.registry { bindings ->
          serverTelemetry.configureRegistry(bindings)
          telemetry.configureRegistry(bindings)
          bindings.bindInstance(HttpClient, telemetry.instrument(
            HttpClient.of { s -> s.readTimeout(Duration.ofMillis(10)) })
          )
        }
      )

      spec.handlers { chain ->
        chain.get("path-name") { ctx ->
          def instrumentedHttpClient = ctx.get(HttpClient)
          instrumentedHttpClient.get(new URI("${otherApp.address}foo"))
            .onError { ctx.render("error") }
            .then { ctx.render("hello") }
        }
      }
    }

    app.test { httpClient ->
      assert "error" == httpClient.get("path-name").body.text

      new PollingConditions().eventually {
        def spanData = spanExporter.finishedSpanItems.find { it.name == "GET /path-name" }
        def spanClientData = spanExporter.finishedSpanItems.find { it.name == "GET" }

        spanData.traceId == spanClientData.traceId

        spanData.kind == SERVER
        spanClientData.kind == CLIENT
        def atts = spanClientData.attributes.asMap()
        atts[HTTP_ROUTE] == "/foo"
        atts[HTTP_REQUEST_METHOD] == "GET"
        atts[HTTP_RESPONSE_STATUS_CODE] == null
        spanClientData.status.statusCode == StatusCode.ERROR
        spanClientData.events.first().name == "exception"

        def attributes = spanData.attributes.asMap()
        attributes[HTTP_ROUTE] == "/path-name"
        attributes[UrlAttributes.URL_PATH] == "/path-name"
        attributes[HTTP_REQUEST_METHOD] == "GET"
        attributes[HTTP_RESPONSE_STATUS_CODE] == 200L
      }
    }
  }

  def "propagate http trace in ratpack services with compute thread"() {
    expect:
    def latch = new CountDownLatch(1)

    def otherApp = EmbeddedApp.of { spec ->
      spec.handlers {
        it.get("foo") { ctx -> ctx.render("bar") }
      }
    }

    def app = EmbeddedApp.of { spec ->
      spec.registry(
        Guice.registry { bindings ->
          serverTelemetry.configureRegistry(bindings)
          telemetry.configureRegistry(bindings)
          bindings.bindInstance(HttpClient, telemetry.instrument(HttpClient.of(Action.noop())))
          bindings.bindInstance(new BarService(latch, "${otherApp.address}foo", openTelemetry))
        },
      )
      spec.handlers { chain ->
        chain.get("foo") { ctx -> ctx.render("bar") }
      }
    }

    app.address
    latch.await()
    new PollingConditions().eventually {
      def spanData = spanExporter.finishedSpanItems.find { it.name == "a-span" }
      def trace = spanExporter.finishedSpanItems.findAll { it.traceId == spanData.traceId }

      trace.size() == 3
    }
  }

  def "propagate http trace in ratpack services with fork executions"() {
    expect:
    def latch = new CountDownLatch(1)

    def otherApp = EmbeddedApp.of { spec ->
      spec.handlers {
        it.get("foo") { ctx -> ctx.render("bar") }
      }
    }

    def app = EmbeddedApp.of { spec ->
      spec.registry(
        Guice.registry { bindings ->
          serverTelemetry.configureRegistry(bindings)
          telemetry.configureRegistry(bindings)
          bindings.bindInstance(HttpClient, telemetry.instrument(HttpClient.of(Action.noop())))
          bindings.bindInstance(new BarForkService(latch, "${otherApp.address}foo", openTelemetry))
        },
      )
      spec.handlers { chain ->
        chain.get("foo") { ctx -> ctx.render("bar") }
      }
    }

    app.address
    latch.await()
    new PollingConditions().eventually {
      def spanData = spanExporter.finishedSpanItems.find { it.name == "a-span" }
      def trace = spanExporter.finishedSpanItems.findAll { it.traceId == spanData.traceId }

      trace.size() == 3
    }
  }
}

class BarService implements Service {
  private final String url
  private final CountDownLatch latch
  private final OpenTelemetry openTelemetry

  BarService(CountDownLatch latch, String url, OpenTelemetry openTelemetry) {
    this.latch = latch
    this.url = url
    this.openTelemetry = openTelemetry
  }

  private Tracer tracer = openTelemetry.tracerProvider.tracerBuilder("testing").build()

  void onStart(StartEvent event) {
    def parentContext = Context.current()
    def span = tracer.spanBuilder("a-span")
      .setParent(parentContext)
      .startSpan()

    Context otelContext = parentContext.with(span)
    otelContext.makeCurrent().withCloseable {
      Execution.current().add(Context, otelContext)
      def httpClient = event.registry.get(HttpClient)
      httpClient.get(new URI(url))
        .flatMap { httpClient.get(new URI(url)) }
        .then {
          span.end()
          latch.countDown()
        }
    }
  }
}

class BarForkService implements Service {
  private final String url
  private final CountDownLatch latch
  private final OpenTelemetry openTelemetry

  BarForkService(CountDownLatch latch, String url, OpenTelemetry openTelemetry) {
    this.latch = latch
    this.url = url
    this.openTelemetry = openTelemetry
  }

  private Tracer tracer = openTelemetry.tracerProvider.tracerBuilder("testing").build()

  void onStart(StartEvent event) {
    Execution.fork().start {
      def parentContext = Context.current()
      def span = tracer.spanBuilder("a-span")
        .setParent(parentContext)
        .startSpan()

      Context otelContext = parentContext.with(span)
      otelContext.makeCurrent().withCloseable {
        Execution.current().add(Context, otelContext)
        def httpClient = event.registry.get(HttpClient)
        httpClient.get(new URI(url))
          .flatMap { httpClient.get(new URI(url)) }
          .then {
            span.end()
            latch.countDown()
          }
      }
    }
  }
}
