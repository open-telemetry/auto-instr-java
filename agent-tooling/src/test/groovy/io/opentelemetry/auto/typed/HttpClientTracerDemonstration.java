package io.opentelemetry.auto.typed;

import io.opentelemetry.context.Scope;

class HttpClientTracerDemonstration {

  private void serverDemonstration() {
    SampleHttpServerTypedTracer tracer = new SampleHttpServerTypedTracer();

    SampleHttpServerTypedSpan span = tracer.startSpan("request instance");
    // span.onRequest("request instance"); // implicitly called on start.

    try (Scope scope = tracer.withSpan(span)) {
      // make request
      String response = "response instance";

      // span.onResponse("response instance"); // implicitly called on end.
      span.end(response);
    } catch (Exception ex) {
      span.end(ex);
    }
  }

  private void clientDemonstration() {
    SampleHttpClientTypedTracer tracer = new SampleHttpClientTypedTracer();

    SampleHttpClientTypedSpan span = tracer.startSpan("request instance");
    // span.onRequest("request instance"); // implicitly called on start.

    try (Scope scope = tracer.withSpan(span)) {
      // make request
      String response = "response instance";

      // span.onResponse("response instance"); // implicitly called on end.
      span.end(response);
    } catch (Exception ex) {
      span.end(ex);
    }
  }
}
