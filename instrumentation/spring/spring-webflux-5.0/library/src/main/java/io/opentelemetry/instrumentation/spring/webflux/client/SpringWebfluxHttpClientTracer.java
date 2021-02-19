/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.spring.webflux.client;

import static io.opentelemetry.instrumentation.spring.webflux.client.HttpHeadersInjectAdapter.SETTER;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.instrumentation.api.tracer.HttpClientTracer;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URI;
import java.util.List;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;

public class SpringWebfluxHttpClientTracer
    extends HttpClientTracer<ClientRequest, ClientRequest.Builder, ClientResponse> {

  private static final SpringWebfluxHttpClientTracer TRACER = new SpringWebfluxHttpClientTracer();

  public static SpringWebfluxHttpClientTracer tracer() {
    return TRACER;
  }

  private static final MethodHandle RAW_STATUS_CODE = findRawStatusCode();

  public void onCancel(Context context) {
    if (captureExperimentalSpanAttributes()) {
      Span span = Span.fromContext(context);
      span.setAttribute("spring-webflux.event", "cancelled");
      span.setAttribute("spring-webflux.message", "The subscription was cancelled");
    }
  }

  @Override
  protected String method(ClientRequest httpRequest) {
    return httpRequest.method().name();
  }

  @Override
  protected URI url(ClientRequest httpRequest) {
    return httpRequest.url();
  }

  @Override
  protected Integer status(ClientResponse httpResponse) {
    if (RAW_STATUS_CODE != null) {
      // rawStatusCode() method was introduced in webflux 5.1
      try {
        return (int) RAW_STATUS_CODE.invokeExact(httpResponse);
      } catch (Throwable ignored) {
      }
    }
    // prior to webflux 5.1, the best we can get is HttpStatus enum, which only covers standard
    // status codes
    return httpResponse.statusCode().value();
  }

  @Override
  protected String requestHeader(ClientRequest clientRequest, String name) {
    return clientRequest.headers().getFirst(name);
  }

  @Override
  protected String responseHeader(ClientResponse clientResponse, String name) {
    List<String> headers = clientResponse.headers().header(name);
    return !headers.isEmpty() ? headers.get(0) : null;
  }

  @Override
  protected TextMapSetter<ClientRequest.Builder> getSetter() {
    return SETTER;
  }

  @Override
  protected String getInstrumentationName() {
    return "io.opentelemetry.javaagent.spring-webflux";
  }

  public Tracer getTracer() {
    return tracer;
  }

  // rawStatusCode() method was introduced in webflux 5.1
  // prior to this method, the best we can get is HttpStatus enum, which only covers standard status
  // codes (see usage above)
  private static MethodHandle findRawStatusCode() {
    try {
      return MethodHandles.publicLookup()
          .findVirtual(ClientResponse.class, "rawStatusCode", MethodType.methodType(int.class));
    } catch (IllegalAccessException | NoSuchMethodException e) {
      return null;
    }
  }

  // TODO cache this after
  //  https://github.com/open-telemetry/opentelemetry-java-instrumentation/pull/1643
  private static boolean captureExperimentalSpanAttributes() {
    return Config.get()
        .getBooleanProperty(
            "otel.instrumentation.spring-webflux.experimental-span-attributes", false);
  }
}
