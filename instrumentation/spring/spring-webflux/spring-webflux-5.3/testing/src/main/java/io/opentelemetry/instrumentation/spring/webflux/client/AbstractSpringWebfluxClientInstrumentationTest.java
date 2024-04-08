/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.spring.webflux.client;

import static io.opentelemetry.api.trace.SpanKind.CLIENT;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static java.util.Collections.emptyMap;
import static java.util.Objects.requireNonNull;
import static org.assertj.core.api.Assertions.catchThrowable;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.testing.junit.http.AbstractHttpClientTest;
import io.opentelemetry.instrumentation.testing.junit.http.HttpClientResult;
import io.opentelemetry.instrumentation.testing.junit.http.HttpClientTestOptions;
import io.opentelemetry.sdk.trace.data.StatusData;
import io.opentelemetry.semconv.SemanticAttributes;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

public abstract class AbstractSpringWebfluxClientInstrumentationTest
    extends AbstractHttpClientTest<WebClient.RequestBodySpec> {

  @Override
  public WebClient.RequestBodySpec buildRequest(
      String method, URI uri, Map<String, String> headers) {

    WebClient webClient =
        instrument(WebClient.builder().clientConnector(ClientHttpConnectorFactory.create()))
            .build();

    return webClient
        .method(HttpMethod.valueOf(method))
        .uri(uri.toString(), Collections.emptyMap())
        .headers(h -> headers.forEach(h::add));
  }

  protected abstract WebClient.Builder instrument(WebClient.Builder builder);

  @Override
  public int sendRequest(
      WebClient.RequestBodySpec request, String method, URI uri, Map<String, String> headers) {
    ClientResponse response = requireNonNull(request.exchange().block());
    return getStatusCode(response);
  }

  @Override
  public void sendRequestWithCallback(
      WebClient.RequestBodySpec request,
      String method,
      URI uri,
      Map<String, String> headers,
      HttpClientResult httpClientResult) {
    request
        .exchange()
        .subscribe(
            response -> httpClientResult.complete(getStatusCode(response)),
            httpClientResult::complete);
  }

  @Override
  protected void configure(HttpClientTestOptions.Builder optionsBuilder) {
    optionsBuilder.disableTestRedirects();

    // no enum value for non standard method
    optionsBuilder.disableTestNonStandardHttpMethod();

    // timeouts leak the scope
    optionsBuilder.disableTestReadTimeout();

    optionsBuilder.setHttpAttributes(
        uri -> {
          Set<AttributeKey<?>> attributes =
              new HashSet<>(HttpClientTestOptions.DEFAULT_HTTP_ATTRIBUTES);
          attributes.remove(SemanticAttributes.NETWORK_PROTOCOL_VERSION);
          return attributes;
        });

    optionsBuilder.setClientSpanErrorMapper(
        (uri, throwable) -> {
          if (!throwable.getClass().getName().endsWith("WebClientRequestException")) {
            String uriString = uri.toString();
            if (uriString.equals("http://localhost:61/")) { // unopened port
              if (!throwable.getClass().getName().endsWith("AnnotatedConnectException")) {
                throwable = throwable.getCause();
              }
            } else if (uriString.equals("https://192.0.2.1/")) { // non routable address
              throwable = throwable.getCause();
            }
          }
          return throwable;
        });

    optionsBuilder.setSingleConnectionFactory(
        (host, port) -> new SpringWebfluxSingleConnection(host, port, this::instrument));
  }

  private static final MethodHandle GET_STATUS_CODE;
  private static final MethodHandle STATUS_CODE_VALUE;

  static {
    MethodHandle getStatusCode;
    MethodHandle statusCodeValue;
    Class<?> httpStatusCodeClass;

    MethodHandles.Lookup lookup = MethodHandles.publicLookup();

    try {
      httpStatusCodeClass = Class.forName("org.springframework.http.HttpStatusCode");
    } catch (ClassNotFoundException e) {
      try {
        httpStatusCodeClass = Class.forName("org.springframework.http.HttpStatus");
      } catch (ClassNotFoundException e2) {
        throw new LinkageError("Did not find neither HttpStatus nor HttpStatusCode class", e2);
      }
    }

    try {
      getStatusCode =
          lookup.findVirtual(
              ClientResponse.class, "statusCode", MethodType.methodType(httpStatusCodeClass));
      statusCodeValue =
          lookup.findVirtual(httpStatusCodeClass, "value", MethodType.methodType(int.class));
    } catch (NoSuchMethodException | IllegalAccessException e) {
      throw new LinkageError("Did not find statusCode() method", e);
    }

    GET_STATUS_CODE = getStatusCode;
    STATUS_CODE_VALUE = statusCodeValue;
  }

  private static int getStatusCode(ClientResponse response) {
    try {
      Object statusCode = GET_STATUS_CODE.invoke(response);
      return (int) STATUS_CODE_VALUE.invoke(statusCode);
    } catch (Throwable e) {
      throw new AssertionError(e);
    }
  }

  @Test
  void shouldEndSpanOnMonoTimeout() {
    URI uri = resolveAddress("/read-timeout");
    Throwable thrown =
        catchThrowable(
            () ->
                testing.runWithSpan(
                    "parent",
                    () ->
                        buildRequest("GET", uri, emptyMap())
                            .exchange()
                            // apply Mono timeout that is way shorter than HTTP request timeout
                            .timeout(Duration.ofSeconds(1))
                            .block()));

    testing.waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span ->
                    span.hasName("parent")
                        .hasKind(SpanKind.INTERNAL)
                        .hasNoParent()
                        .hasStatus(StatusData.error())
                        .hasException(thrown),
                span ->
                    span.hasName("GET")
                        .hasKind(CLIENT)
                        .hasParent(trace.getSpan(0))
                        .hasAttributesSatisfyingExactly(
                            equalTo(SemanticAttributes.HTTP_REQUEST_METHOD, "GET"),
                            equalTo(SemanticAttributes.URL_FULL, uri.toString()),
                            equalTo(SemanticAttributes.HTTP_ROUTE, uri.getPath()),
                            equalTo(SemanticAttributes.SERVER_ADDRESS, "localhost"),
                            equalTo(SemanticAttributes.SERVER_PORT, uri.getPort()),
                            equalTo(SemanticAttributes.ERROR_TYPE, "cancelled")),
                span ->
                    span.hasName("test-http-server")
                        .hasKind(SpanKind.SERVER)
                        .hasParent(trace.getSpan(1))));
  }
}
