/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.semconv.http;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.entry;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.internal.HttpConstants;
import io.opentelemetry.semconv.ClientAttributes;
import io.opentelemetry.semconv.ErrorAttributes;
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.NetworkAttributes;
import io.opentelemetry.semconv.ServerAttributes;
import io.opentelemetry.semconv.UrlAttributes;
import io.opentelemetry.semconv.UserAgentAttributes;
import java.net.ConnectException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.ValueSource;

class HttpServerAttributesExtractorTest {

  static class TestHttpServerAttributesGetter
      implements HttpServerAttributesGetter<Map<String, String>, Map<String, String>> {

    @Override
    public String getHttpRequestMethod(Map<String, String> request) {
      return request.get("method");
    }

    @Override
    public String getUrlScheme(Map<String, String> request) {
      return request.get("urlScheme");
    }

    @Nullable
    @Override
    public String getUrlPath(Map<String, String> request) {
      return request.get("urlPath");
    }

    @Nullable
    @Override
    public String getUrlQuery(Map<String, String> request) {
      return request.get("urlQuery");
    }

    @Override
    public String getHttpRoute(Map<String, String> request) {
      return request.get("route");
    }

    @Override
    public List<String> getHttpRequestHeader(Map<String, String> request, String name) {
      String values = request.get("header." + name);
      return values == null ? emptyList() : asList(values.split(","));
    }

    @Override
    public Integer getHttpResponseStatusCode(
        Map<String, String> request, Map<String, String> response, @Nullable Throwable error) {
      String value = response.get("statusCode");
      return value == null ? null : Integer.parseInt(value);
    }

    @Override
    public List<String> getHttpResponseHeader(
        Map<String, String> request, Map<String, String> response, String name) {
      String values = response.get("header." + name);
      return values == null ? emptyList() : asList(values.split(","));
    }

    @Nullable
    @Override
    public String getNetworkTransport(
        Map<String, String> request, @Nullable Map<String, String> response) {
      return request.get("networkTransport");
    }

    @Nullable
    @Override
    public String getNetworkType(
        Map<String, String> request, @Nullable Map<String, String> response) {
      return request.get("networkType");
    }

    @Nullable
    @Override
    public String getNetworkProtocolName(
        Map<String, String> request, Map<String, String> response) {
      return request.get("networkProtocolName");
    }

    @Nullable
    @Override
    public String getNetworkProtocolVersion(
        Map<String, String> request, Map<String, String> response) {
      return request.get("networkProtocolVersion");
    }

    @Nullable
    @Override
    public String getNetworkLocalAddress(
        Map<String, String> request, @Nullable Map<String, String> response) {
      return request.get("networkLocalAddress");
    }

    @Nullable
    @Override
    public Integer getNetworkLocalPort(
        Map<String, String> request, @Nullable Map<String, String> response) {
      String value = request.get("networkLocalPort");
      return value == null ? null : Integer.parseInt(value);
    }

    @Nullable
    @Override
    public String getNetworkPeerAddress(
        Map<String, String> request, @Nullable Map<String, String> response) {
      return request.get("networkPeerAddress");
    }

    @Nullable
    @Override
    public Integer getNetworkPeerPort(
        Map<String, String> request, @Nullable Map<String, String> response) {
      String value = request.get("networkPeerPort");
      return value == null ? null : Integer.parseInt(value);
    }

    @Nullable
    @Override
    public String getErrorType(
        Map<String, String> request,
        @Nullable Map<String, String> respobse,
        @Nullable Throwable error) {
      return request.get("errorType");
    }
  }

  @Test
  void normal() {
    Map<String, String> request = new HashMap<>();
    request.put("method", "POST");
    request.put("urlFull", "https://github.com");
    request.put("urlPath", "/repositories/1");
    request.put("urlQuery", "details=true");
    request.put("urlScheme", "https");
    request.put("header.content-length", "10");
    request.put("route", "/repositories/{id}");
    request.put("header.user-agent", "okhttp 3.x");
    request.put("header.host", "github.com:443");
    request.put("header.forwarded", "for=1.1.1.1;proto=https");
    request.put("header.custom-request-header", "123,456");
    request.put("networkTransport", "udp");
    request.put("networkType", "ipv4");
    request.put("networkProtocolName", "http");
    request.put("networkProtocolVersion", "2.0");
    request.put("networkLocalAddress", "1.2.3.4");
    request.put("networkLocalPort", "42");
    request.put("networkPeerAddress", "4.3.2.1");
    request.put("networkPeerPort", "456");

    Map<String, String> response = new HashMap<>();
    response.put("statusCode", "202");
    response.put("header.content-length", "20");
    response.put("header.custom-response-header", "654,321");

    Function<Context, String> routeFromContext = ctx -> "/repositories/{repoId}";

    AttributesExtractor<Map<String, String>, Map<String, String>> extractor =
        HttpServerAttributesExtractor.builder(new TestHttpServerAttributesGetter())
            .setCapturedRequestHeaders(singletonList("Custom-Request-Header"))
            .setCapturedResponseHeaders(singletonList("Custom-Response-Header"))
            .setHttpRouteGetter(routeFromContext)
            .build();

    AttributesBuilder startAttributes = Attributes.builder();
    extractor.onStart(startAttributes, Context.root(), request);

    AttributesBuilder endAttributes = Attributes.builder();
    extractor.onEnd(endAttributes, Context.root(), request, response, null);
    assertThat(endAttributes.build())
        .containsOnly(
            entry(NetworkAttributes.NETWORK_PROTOCOL_VERSION, "2.0"),
            entry(NetworkAttributes.NETWORK_PEER_ADDRESS, "4.3.2.1"),
            entry(NetworkAttributes.NETWORK_PEER_PORT, 456L),
            entry(HttpAttributes.HTTP_ROUTE, "/repositories/{repoId}"),
            entry(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 202L),
            entry(
                AttributeKey.stringArrayKey("http.response.header.custom-response-header"),
                asList("654", "321")),
                entry(ServerAttributes.SERVER_ADDRESS, "github.com"),
                entry(ServerAttributes.SERVER_PORT, 443L),
                entry(HttpAttributes.HTTP_REQUEST_METHOD, "POST"),
                entry(UrlAttributes.URL_SCHEME, "https"),
                entry(UrlAttributes.URL_PATH, "/repositories/1"),
                entry(UrlAttributes.URL_QUERY, "details=true"),
                entry(UserAgentAttributes.USER_AGENT_ORIGINAL, "okhttp 3.x"),
                //entry(HttpAttributes.HTTP_ROUTE, "/repositories/{id}"),
                entry(ClientAttributes.CLIENT_ADDRESS, "1.1.1.1"),
                entry(
                        AttributeKey.stringArrayKey("http.request.header.custom-request-header"),
                        asList("123", "456")));
  }

  @ParameterizedTest
  @ArgumentsSource(ValidRequestMethodsProvider.class)
  void shouldExtractKnownMethods(String requestMethod) {
    Map<String, String> request = new HashMap<>();
    request.put("method", requestMethod);

    AttributesExtractor<Map<String, String>, Map<String, String>> extractor =
        HttpServerAttributesExtractor.create(new TestHttpServerAttributesGetter());

    AttributesBuilder attributes = Attributes.builder();
    extractor.onStart(attributes, Context.root(), request);
    extractor.onEnd(attributes, Context.root(), request, emptyMap(), null);

    assertThat(attributes.build())
        .containsEntry(HttpAttributes.HTTP_REQUEST_METHOD, requestMethod)
        .doesNotContainKey(HttpAttributes.HTTP_REQUEST_METHOD_ORIGINAL);
  }

  @ParameterizedTest
  @ValueSource(strings = {"get", "Get"})
  void shouldTreatMethodsAsCaseSensitive(String requestMethod) {
    Map<String, String> request = new HashMap<>();
    request.put("method", requestMethod);

    AttributesExtractor<Map<String, String>, Map<String, String>> extractor =
        HttpServerAttributesExtractor.create(new TestHttpServerAttributesGetter());

    AttributesBuilder attributes = Attributes.builder();
    extractor.onStart(attributes, Context.root(), request);
    extractor.onEnd(attributes, Context.root(), request, emptyMap(), null);

    assertThat(attributes.build())
        .containsEntry(HttpAttributes.HTTP_REQUEST_METHOD, HttpConstants._OTHER)
        .containsEntry(HttpAttributes.HTTP_REQUEST_METHOD_ORIGINAL, requestMethod);
  }

  @ParameterizedTest
  @ValueSource(strings = {"PURGE", "not a method really"})
  void shouldUseOtherForUnknownMethods(String requestMethod) {
    Map<String, String> request = new HashMap<>();
    request.put("method", requestMethod);

    AttributesExtractor<Map<String, String>, Map<String, String>> extractor =
        HttpServerAttributesExtractor.create(new TestHttpServerAttributesGetter());

    AttributesBuilder attributes = Attributes.builder();
    extractor.onStart(attributes, Context.root(), request);
    extractor.onEnd(attributes, Context.root(), request, emptyMap(), null);

    assertThat(attributes.build())
        .containsEntry(HttpAttributes.HTTP_REQUEST_METHOD, HttpConstants._OTHER)
        .containsEntry(HttpAttributes.HTTP_REQUEST_METHOD_ORIGINAL, requestMethod);
  }

  @ParameterizedTest
  @ValueSource(strings = {"only", "custom", "methods", "allowed"})
  void shouldExtractKnownMethods_override(String requestMethod) {
    Map<String, String> request = new HashMap<>();
    request.put("method", requestMethod);

    AttributesExtractor<Map<String, String>, Map<String, String>> extractor =
        HttpServerAttributesExtractor.builder(new TestHttpServerAttributesGetter())
            .setKnownMethods(new HashSet<>(asList("only", "custom", "methods", "allowed")))
            .build();

    AttributesBuilder attributes = Attributes.builder();
    extractor.onStart(attributes, Context.root(), request);
    extractor.onEnd(attributes, Context.root(), request, emptyMap(), null);

    assertThat(attributes.build())
        .containsEntry(HttpAttributes.HTTP_REQUEST_METHOD, requestMethod)
        .doesNotContainKey(HttpAttributes.HTTP_REQUEST_METHOD_ORIGINAL);
  }

  @ParameterizedTest
  @ArgumentsSource(ValidRequestMethodsProvider.class)
  void shouldUseOtherForUnknownMethods_override(String requestMethod) {
    Map<String, String> request = new HashMap<>();
    request.put("method", requestMethod);

    AttributesExtractor<Map<String, String>, Map<String, String>> extractor =
        HttpServerAttributesExtractor.builder(new TestHttpServerAttributesGetter())
            .setKnownMethods(new HashSet<>(asList("only", "custom", "methods", "allowed")))
            .build();

    AttributesBuilder attributes = Attributes.builder();
    extractor.onStart(attributes, Context.root(), request);
    extractor.onEnd(attributes, Context.root(), request, emptyMap(), null);

    assertThat(attributes.build())
        .containsEntry(HttpAttributes.HTTP_REQUEST_METHOD, HttpConstants._OTHER)
        .containsEntry(HttpAttributes.HTTP_REQUEST_METHOD_ORIGINAL, requestMethod);
  }

  @Test
  void shouldExtractErrorType_httpStatusCode() {
    Map<String, String> response = new HashMap<>();
    response.put("statusCode", "500");

    AttributesExtractor<Map<String, String>, Map<String, String>> extractor =
        HttpServerAttributesExtractor.create(new TestHttpServerAttributesGetter());

    AttributesBuilder attributes = Attributes.builder();
    extractor.onStart(attributes, Context.root(), emptyMap());
    extractor.onEnd(attributes, Context.root(), emptyMap(), response, null);

    assertThat(attributes.build())
        .containsEntry(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 500)
        .containsEntry(ErrorAttributes.ERROR_TYPE, "500");
  }

  @Test
  void shouldExtractErrorType_getter() {
    Map<String, String> request = new HashMap<>();
    request.put("statusCode", "0");
    request.put("errorType", "custom error type");

    AttributesExtractor<Map<String, String>, Map<String, String>> extractor =
        HttpServerAttributesExtractor.create(new TestHttpServerAttributesGetter());

    AttributesBuilder attributes = Attributes.builder();
    extractor.onStart(attributes, Context.root(), emptyMap());
    extractor.onEnd(attributes, Context.root(), request, emptyMap(), null);

    assertThat(attributes.build()).containsEntry(ErrorAttributes.ERROR_TYPE, "custom error type");
  }

  @Test
  void shouldExtractErrorType_exceptionClassName() {
    AttributesExtractor<Map<String, String>, Map<String, String>> extractor =
        HttpServerAttributesExtractor.create(new TestHttpServerAttributesGetter());

    AttributesBuilder attributes = Attributes.builder();
    extractor.onStart(attributes, Context.root(), emptyMap());
    extractor.onEnd(attributes, Context.root(), emptyMap(), emptyMap(), new ConnectException());

    assertThat(attributes.build())
        .containsEntry(ErrorAttributes.ERROR_TYPE, "java.net.ConnectException");
  }

  @Test
  void shouldExtractErrorType_other() {
    AttributesExtractor<Map<String, String>, Map<String, String>> extractor =
        HttpServerAttributesExtractor.create(new TestHttpServerAttributesGetter());

    AttributesBuilder attributes = Attributes.builder();
    extractor.onStart(attributes, Context.root(), emptyMap());
    extractor.onEnd(attributes, Context.root(), emptyMap(), emptyMap(), null);

    assertThat(attributes.build()).containsEntry(ErrorAttributes.ERROR_TYPE, HttpConstants._OTHER);
  }

  @Test
  void shouldPreferUrlSchemeFromForwardedHeader() {
    Map<String, String> request = new HashMap<>();
    request.put("urlScheme", "http");
    request.put("header.forwarded", "proto=https");

    Map<String, String> response = new HashMap<>();
    response.put("statusCode", "202");

    AttributesExtractor<Map<String, String>, Map<String, String>> extractor =
        HttpServerAttributesExtractor.create(new TestHttpServerAttributesGetter());

    AttributesBuilder startAttributes = Attributes.builder();
    extractor.onStart(startAttributes, Context.root(), request);
    //assertThat(startAttributes.build()).containsOnly(entry(UrlAttributes.URL_SCHEME, "https"));

    AttributesBuilder endAttributes = Attributes.builder();
    extractor.onEnd(endAttributes, Context.root(), request, response, null);
    assertThat(endAttributes.build())
        .containsOnly(entry(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 202L),
                entry(UrlAttributes.URL_SCHEME, "https"));
  }

  @Test
  void shouldExtractServerAddressAndPortFromForwardedHeader() {
    Map<String, String> request = new HashMap<>();
    request.put("header.forwarded", "host=example.com:42");
    request.put("header.x-forwarded-host", "opentelemetry.io:987");
    request.put("header.host", "github.com:123");
    request.put("header.:authority", "opentelemetry.io:456");

    Map<String, String> response = new HashMap<>();
    response.put("statusCode", "200");

    AttributesExtractor<Map<String, String>, Map<String, String>> extractor =
        HttpServerAttributesExtractor.create(new TestHttpServerAttributesGetter());

    AttributesBuilder startAttributes = Attributes.builder();
    extractor.onStart(startAttributes, Context.root(), request);

    AttributesBuilder endAttributes = Attributes.builder();
    extractor.onEnd(endAttributes, Context.root(), request, response, null);
    assertThat(endAttributes.build())
        .containsOnly(entry(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 200L),
                entry(ServerAttributes.SERVER_ADDRESS, "example.com"),
                entry(ServerAttributes.SERVER_PORT, 42L));
  }

  @Test
  void shouldExtractServerAddressAndPortFromForwardedHostHeader() {
    Map<String, String> request = new HashMap<>();
    request.put("header.x-forwarded-host", "opentelemetry.io:987");
    request.put("header.host", "github.com:123");
    request.put("header.:authority", "opentelemetry.io:42");

    Map<String, String> response = new HashMap<>();
    response.put("statusCode", "200");

    AttributesExtractor<Map<String, String>, Map<String, String>> extractor =
        HttpServerAttributesExtractor.create(new TestHttpServerAttributesGetter());

    AttributesBuilder startAttributes = Attributes.builder();
    extractor.onStart(startAttributes, Context.root(), request);

    AttributesBuilder endAttributes = Attributes.builder();
    extractor.onEnd(endAttributes, Context.root(), request, response, null);
    assertThat(endAttributes.build())
        .containsOnly(entry(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 200L),
                entry(ServerAttributes.SERVER_ADDRESS, "opentelemetry.io"),
                entry(ServerAttributes.SERVER_PORT, 987L));
  }

  @Test
  void shouldExtractServerAddressAndPortFromAuthorityPseudoHeader() {
    Map<String, String> request = new HashMap<>();
    request.put("header.:authority", "opentelemetry.io:42");
    request.put("header.host", "github.com:123");

    Map<String, String> response = new HashMap<>();
    response.put("statusCode", "200");

    AttributesExtractor<Map<String, String>, Map<String, String>> extractor =
        HttpServerAttributesExtractor.create(new TestHttpServerAttributesGetter());

    AttributesBuilder startAttributes = Attributes.builder();
    extractor.onStart(startAttributes, Context.root(), request);

    AttributesBuilder endAttributes = Attributes.builder();
    extractor.onEnd(endAttributes, Context.root(), request, response, null);
    assertThat(endAttributes.build())
        .containsOnly(entry(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 200L),
                entry(ServerAttributes.SERVER_ADDRESS, "opentelemetry.io"),
                entry(ServerAttributes.SERVER_PORT, 42L));
  }

  @Test
  void shouldExtractServerAddressAndPortFromHostHeader() {
    Map<String, String> request = new HashMap<>();
    request.put("header.host", "github.com:123");

    Map<String, String> response = new HashMap<>();
    response.put("statusCode", "200");

    AttributesExtractor<Map<String, String>, Map<String, String>> extractor =
        HttpServerAttributesExtractor.create(new TestHttpServerAttributesGetter());

    AttributesBuilder startAttributes = Attributes.builder();
    extractor.onStart(startAttributes, Context.root(), request);

    AttributesBuilder endAttributes = Attributes.builder();
    extractor.onEnd(endAttributes, Context.root(), request, response, null);
    assertThat(endAttributes.build())
        .containsOnly(entry(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 200L),
                entry(ServerAttributes.SERVER_ADDRESS, "github.com"),
                entry(ServerAttributes.SERVER_PORT, 123L));
  }

  @Test
  void shouldExtractPeerAddressEvenIfItDuplicatesClientAddress() {
    Map<String, String> request = new HashMap<>();
    request.put("networkPeerAddress", "1.2.3.4");
    request.put("networkPeerPort", "456");
    request.put("header.forwarded", "for=1.2.3.4:123");

    Map<String, String> response = new HashMap<>();
    response.put("statusCode", "200");

    AttributesExtractor<Map<String, String>, Map<String, String>> extractor =
        HttpServerAttributesExtractor.create(new TestHttpServerAttributesGetter());

    AttributesBuilder startAttributes = Attributes.builder();
    extractor.onStart(startAttributes, Context.root(), request);

    AttributesBuilder endAttributes = Attributes.builder();
    extractor.onEnd(endAttributes, Context.root(), request, response, null);
    assertThat(endAttributes.build())
        .containsOnly(
            entry(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 200L),
            entry(NetworkAttributes.NETWORK_PEER_ADDRESS, "1.2.3.4"),
            entry(NetworkAttributes.NETWORK_PEER_PORT, 456L),
                entry(ClientAttributes.CLIENT_ADDRESS, "1.2.3.4"));
  }

  @Test
  void shouldExtractProtocolNameDifferentFromHttp() {
    Map<String, String> request = new HashMap<>();
    request.put("networkProtocolName", "spdy");
    request.put("networkProtocolVersion", "3.1");

    Map<String, String> response = new HashMap<>();
    response.put("statusCode", "200");

    AttributesExtractor<Map<String, String>, Map<String, String>> extractor =
        HttpServerAttributesExtractor.create(new TestHttpServerAttributesGetter());

    AttributesBuilder startAttributes = Attributes.builder();
    extractor.onStart(startAttributes, Context.root(), request);
    assertThat(startAttributes.build()).isEmpty();

    AttributesBuilder endAttributes = Attributes.builder();
    extractor.onEnd(endAttributes, Context.root(), request, response, null);
    assertThat(endAttributes.build())
        .containsOnly(
            entry(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 200L),
            entry(NetworkAttributes.NETWORK_PROTOCOL_NAME, "spdy"),
            entry(NetworkAttributes.NETWORK_PROTOCOL_VERSION, "3.1"));
  }
}
