/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.instrumenter.http;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.semconv.trace.attributes.SemanticAttributes.NetTransportValues.IP_TCP;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.net.NetClientAttributesGetter;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

class HttpClientAttributesExtractorTest {

  static class TestHttpClientAttributesGetter
      implements HttpClientAttributesGetter<Map<String, String>, Map<String, String>> {

    @Override
    public String method(Map<String, String> request) {
      return request.get("method");
    }

    @Override
    public String url(Map<String, String> request) {
      return request.get("url");
    }

    @Override
    public List<String> requestHeader(Map<String, String> request, String name) {
      String value = request.get("header." + name);
      return value == null ? emptyList() : asList(value.split(","));
    }

    @Override
    public Integer statusCode(
        Map<String, String> request, Map<String, String> response, @Nullable Throwable error) {
      return Integer.parseInt(response.get("statusCode"));
    }

    @Override
    public String flavor(Map<String, String> request, Map<String, String> response) {
      return request.get("flavor");
    }

    @Override
    public List<String> responseHeader(
        Map<String, String> request, Map<String, String> response, String name) {
      String value = response.get("header." + name);
      return value == null ? emptyList() : asList(value.split(","));
    }
  }

  static class TestNetClientAttributesGetter
      implements NetClientAttributesGetter<Map<String, String>, Map<String, String>> {

    @Nullable
    @Override
    public String transport(Map<String, String> request, @Nullable Map<String, String> response) {
      return response.get("transport");
    }

    @Nullable
    @Override
    public String peerName(Map<String, String> request) {
      return request.get("peerName");
    }

    @Nullable
    @Override
    public Integer peerPort(Map<String, String> request) {
      String statusCode = request.get("peerPort");
      return statusCode == null ? null : Integer.parseInt(statusCode);
    }
  }

  @Test
  void normal() {
    Map<String, String> request = new HashMap<>();
    request.put("method", "POST");
    request.put("url", "http://github.com");
    request.put("header.content-length", "10");
    request.put("flavor", "http/2");
    request.put("header.user-agent", "okhttp 3.x");
    request.put("header.custom-request-header", "123,456");
    request.put("peerName", "github.com");
    request.put("peerPort", "123");

    Map<String, String> response = new HashMap<>();
    response.put("statusCode", "202");
    response.put("header.content-length", "20");
    response.put("header.custom-response-header", "654,321");
    response.put("transport", IP_TCP);

    HttpClientAttributesExtractor<Map<String, String>, Map<String, String>> extractor =
        HttpClientAttributesExtractor.builder(
                new TestHttpClientAttributesGetter(), new TestNetClientAttributesGetter())
            .setCapturedRequestHeaders(singletonList("Custom-Request-Header"))
            .setCapturedResponseHeaders(singletonList("Custom-Response-Header"))
            .build();

    AttributesBuilder startAttributes = Attributes.builder();
    extractor.onStart(startAttributes, Context.root(), request);
    assertThat(startAttributes.build())
        .containsOnly(
            entry(SemanticAttributes.HTTP_METHOD, "POST"),
            entry(SemanticAttributes.HTTP_URL, "http://github.com"),
            entry(SemanticAttributes.HTTP_USER_AGENT, "okhttp 3.x"),
            entry(
                AttributeKey.stringArrayKey("http.request.header.custom_request_header"),
                asList("123", "456")),
            entry(SemanticAttributes.NET_PEER_NAME, "github.com"),
            entry(SemanticAttributes.NET_PEER_PORT, 123L));

    AttributesBuilder endAttributes = Attributes.builder();
    extractor.onEnd(endAttributes, Context.root(), request, response, null);
    assertThat(endAttributes.build())
        .containsOnly(
            entry(SemanticAttributes.HTTP_REQUEST_CONTENT_LENGTH, 10L),
            entry(SemanticAttributes.HTTP_FLAVOR, "http/2"),
            entry(SemanticAttributes.HTTP_STATUS_CODE, 202L),
            entry(SemanticAttributes.HTTP_RESPONSE_CONTENT_LENGTH, 20L),
            entry(
                AttributeKey.stringArrayKey("http.response.header.custom_response_header"),
                asList("654", "321")),
            entry(SemanticAttributes.NET_TRANSPORT, IP_TCP));
  }

  @ParameterizedTest
  @ArgumentsSource(StripUrlArgumentSource.class)
  void stripBasicAuthTest(String url, String expectedResult) {
    Map<String, String> request = new HashMap<>();
    request.put("url", url);

    HttpClientAttributesExtractor<Map<String, String>, Map<String, String>> extractor =
        HttpClientAttributesExtractor.builder(
                new TestHttpClientAttributesGetter(), new TestNetClientAttributesGetter())
            .build();

    AttributesBuilder attributes = Attributes.builder();
    extractor.onStart(attributes, Context.root(), request);

    assertThat(attributes.build()).containsOnly(entry(SemanticAttributes.HTTP_URL, expectedResult));
  }

  static final class StripUrlArgumentSource implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
      return Stream.of(
          arguments("https://user1:secret@github.com", "https://github.com"),
          arguments("https://user1:secret@github.com/path/", "https://github.com/path/"),
          arguments("https://user1:secret@github.com#test.html", "https://github.com#test.html"),
          arguments("https://user1:secret@github.com?foo=b@r", "https://github.com?foo=b@r"),
          arguments(
              "https://user1:secret@github.com/p@th?foo=b@r", "https://github.com/p@th?foo=b@r"),
          arguments("https://github.com/p@th?foo=b@r", "https://github.com/p@th?foo=b@r"),
          arguments("https://github.com#t@st.html", "https://github.com#t@st.html"),
          arguments("user1:secret@github.com", "user1:secret@github.com"),
          arguments("https://github.com@", "https://github.com@"));
    }
  }

  @Test
  void invalidStatusCode() {
    Map<String, String> request = new HashMap<>();

    Map<String, String> response = new HashMap<>();
    response.put("statusCode", "0");

    HttpClientAttributesExtractor<Map<String, String>, Map<String, String>> extractor =
        HttpClientAttributesExtractor.builder(
                new TestHttpClientAttributesGetter(), new TestNetClientAttributesGetter())
            .setCapturedRequestHeaders(emptyList())
            .setCapturedResponseHeaders(emptyList())
            .build();

    AttributesBuilder attributes = Attributes.builder();
    extractor.onStart(attributes, Context.root(), request);
    assertThat(attributes.build()).isEmpty();

    extractor.onEnd(attributes, Context.root(), request, response, null);
    assertThat(attributes.build()).isEmpty();
  }

  @ParameterizedTest
  @ArgumentsSource(DefaultPeerPortArgumentSource.class)
  void defaultPeerPort(int peerPort, String url) {
    Map<String, String> request = new HashMap<>();
    request.put("url", url);
    request.put("peerPort", String.valueOf(peerPort));

    HttpClientAttributesExtractor<Map<String, String>, Map<String, String>> extractor =
        HttpClientAttributesExtractor.builder(
                new TestHttpClientAttributesGetter(), new TestNetClientAttributesGetter())
            .build();

    AttributesBuilder attributes = Attributes.builder();
    extractor.onStart(attributes, Context.root(), request);

    assertThat(attributes.build()).doesNotContainKey(SemanticAttributes.NET_PEER_PORT);
  }

  static class DefaultPeerPortArgumentSource implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
      return Stream.of(arguments(80, "http://github.com"), arguments(443, "https://github.com"));
    }
  }
}
