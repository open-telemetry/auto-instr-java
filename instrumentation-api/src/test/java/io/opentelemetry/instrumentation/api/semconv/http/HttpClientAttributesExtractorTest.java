/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.semconv.http;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.semconv.ErrorAttributes.ERROR_TYPE;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_REQUEST_METHOD;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_REQUEST_METHOD_ORIGINAL;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_REQUEST_RESEND_COUNT;
import static io.opentelemetry.semconv.HttpAttributes.HTTP_RESPONSE_STATUS_CODE;
import static io.opentelemetry.semconv.NetworkAttributes.NETWORK_PEER_ADDRESS;
import static io.opentelemetry.semconv.NetworkAttributes.NETWORK_PEER_PORT;
import static io.opentelemetry.semconv.NetworkAttributes.NETWORK_PROTOCOL_NAME;
import static io.opentelemetry.semconv.NetworkAttributes.NETWORK_PROTOCOL_VERSION;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_ADDRESS;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_PORT;
import static io.opentelemetry.semconv.UrlAttributes.URL_FULL;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.entry;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.internal.Experimental;
import io.opentelemetry.instrumentation.api.internal.HttpConstants;
import java.net.ConnectException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.function.ToIntFunction;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.provider.ValueSource;

class HttpClientAttributesExtractorTest {

  static class TestHttpClientAttributesGetter
      implements HttpClientAttributesGetter<Map<String, String>, Map<String, String>> {

    @Override
    public String getUrlFull(Map<String, String> request) {
      return request.get("urlFull");
    }

    @Override
    public String getHttpRequestMethod(Map<String, String> request) {
      return request.get("method");
    }

    @Override
    public List<String> getHttpRequestHeader(Map<String, String> request, String name) {
      String value = request.get("header." + name);
      return value == null ? emptyList() : asList(value.split(","));
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
      String value = response.get("header." + name);
      return value == null ? emptyList() : asList(value.split(","));
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
        Map<String, String> request, @Nullable Map<String, String> response) {
      return request.get("networkProtocolName");
    }

    @Nullable
    @Override
    public String getNetworkProtocolVersion(
        Map<String, String> request, @Nullable Map<String, String> response) {
      return request.get("networkProtocolVersion");
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
    public String getServerAddress(Map<String, String> request) {
      return request.get("serverAddress");
    }

    @Nullable
    @Override
    public Integer getServerPort(Map<String, String> request) {
      String value = request.get("serverPort");
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
    request.put("urlFull", "http://github.com");
    request.put("header.content-length", "10");
    request.put("header.user-agent", "okhttp 3.x");
    request.put("header.custom-request-header", "123,456");
    request.put("networkTransport", "udp");
    request.put("networkType", "ipv4");
    request.put("networkProtocolName", "http");
    request.put("networkProtocolVersion", "1.1");
    request.put("networkPeerAddress", "4.3.2.1");
    request.put("networkPeerPort", "456");
    request.put("serverAddress", "github.com");
    request.put("serverPort", "80");

    Map<String, String> response = new HashMap<>();
    response.put("statusCode", "202");
    response.put("header.content-length", "20");
    response.put("header.custom-response-header", "654,321");

    ToIntFunction<Context> resendCountFromContext = context -> 2;

    AttributesExtractor<Map<String, String>, Map<String, String>> extractor =
        HttpClientAttributesExtractor.builder(new TestHttpClientAttributesGetter())
            .setCapturedRequestHeaders(singletonList("Custom-Request-Header"))
            .setCapturedResponseHeaders(singletonList("Custom-Response-Header"))
            .setResendCountIncrementer(resendCountFromContext)
            .build();

    AttributesBuilder startAttributes = Attributes.builder();
    extractor.onStart(startAttributes, Context.root(), request);
    assertThat(startAttributes.build())
        .containsOnly(
            entry(HTTP_REQUEST_METHOD, "POST"),
            entry(URL_FULL, "http://github.com"),
            entry(
                AttributeKey.stringArrayKey("http.request.header.custom-request-header"),
                asList("123", "456")),
            entry(SERVER_ADDRESS, "github.com"),
            entry(SERVER_PORT, 80L),
            entry(HTTP_REQUEST_RESEND_COUNT, 2L));

    AttributesBuilder endAttributes = Attributes.builder();
    extractor.onEnd(endAttributes, Context.root(), request, response, null);
    assertThat(endAttributes.build())
        .containsOnly(
            entry(HTTP_RESPONSE_STATUS_CODE, 202L),
            entry(
                AttributeKey.stringArrayKey("http.response.header.custom-response-header"),
                asList("654", "321")),
            entry(NETWORK_PROTOCOL_VERSION, "1.1"),
            entry(NETWORK_PEER_ADDRESS, "4.3.2.1"),
            entry(NETWORK_PEER_PORT, 456L));
  }

  @ParameterizedTest
  @ArgumentsSource(UrlSourceToRedact.class)
  void shouldRedactUserInfoAndQueryParameters(String url, String expectedResult) {
    Map<String, String> request = new HashMap<>();
    request.put("urlFull", url);

    HttpClientAttributesExtractorBuilder<Map<String, String>, Map<String, String>> builder =
        HttpClientAttributesExtractor.builder(new TestHttpClientAttributesGetter());
    Experimental.setRedactQueryParameters(builder, true);
    AttributesExtractor<Map<String, String>, Map<String, String>> extractor = builder.build();

    AttributesBuilder attributes = Attributes.builder();
    extractor.onStart(attributes, Context.root(), request);

    assertThat(attributes.build()).containsOnly(entry(URL_FULL, expectedResult));
  }

  static final class UrlSourceToRedact implements ArgumentsProvider {

    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext context) {
      return Stream.of(
          arguments("https://user1:secret@github.com", "https://REDACTED:REDACTED@github.com"),
          arguments(
              "https://user1:secret@github.com/path/",
              "https://REDACTED:REDACTED@github.com/path/"),
          arguments(
              "https://user1:secret@github.com#test.html",
              "https://REDACTED:REDACTED@github.com#test.html"),
          arguments(
              "https://user1:secret@github.com?foo=b@r",
              "https://REDACTED:REDACTED@github.com?foo=b@r"),
          arguments(
              "https://user1:secret@github.com/p@th?foo=b@r",
              "https://REDACTED:REDACTED@github.com/p@th?foo=b@r"),
          arguments("https://github.com/p@th?foo=b@r", "https://github.com/p@th?foo=b@r"),
          arguments("https://github.com#t@st.html", "https://github.com#t@st.html"),
          arguments("user1:secret@github.com", "user1:secret@github.com"),
          arguments("https://github.com@", "https://github.com@"),
          arguments(
              "https://service.com?paramA=valA&paramB=valB",
              "https://service.com?paramA=valA&paramB=valB"),
          arguments(
              "https://service.com?AWSAccessKeyId=AKIAIOSFODNN7",
              "https://service.com?AWSAccessKeyId=REDACTED"),
          arguments(
              "https://service.com?Signature=39Up9jzHkxhuIhFE9594DJxe7w6cIRCg0V6ICGS0%3A377",
              "https://service.com?Signature=REDACTED"),
          arguments(
              "https://service.com?sig=39Up9jzHkxhuIhFE9594DJxe7w6cIRCg0V6ICGS0",
              "https://service.com?sig=REDACTED"),
          arguments(
              "https://service.com?X-Goog-Signature=39Up9jzHkxhuIhFE9594DJxe7w6cIRCg0V6ICGS0",
              "https://service.com?X-Goog-Signature=REDACTED"),
          arguments(
              "https://service.com?paramA=valA&AWSAccessKeyId=AKIAIOSFODNN7&paramB=valB",
              "https://service.com?paramA=valA&AWSAccessKeyId=REDACTED&paramB=valB"),
          arguments(
              "https://service.com?AWSAccessKeyId=AKIAIOSFODNN7&paramA=valA",
              "https://service.com?AWSAccessKeyId=REDACTED&paramA=valA"),
          arguments(
              "https://service.com?paramA=valA&AWSAccessKeyId=AKIAIOSFODNN7",
              "https://service.com?paramA=valA&AWSAccessKeyId=REDACTED"),
          arguments(
              "https://service.com?AWSAccessKeyId=AKIAIOSFODNN7&AWSAccessKeyId=ZGIAIOSFODNN7",
              "https://service.com?AWSAccessKeyId=REDACTED&AWSAccessKeyId=REDACTED"),
          arguments(
              "https://service.com?AWSAccessKeyId=AKIAIOSFODNN7#ref",
              "https://service.com?AWSAccessKeyId=REDACTED#ref"),
          arguments(
              "https://service.com?AWSAccessKeyId=AKIAIOSFODNN7&aa&bb",
              "https://service.com?AWSAccessKeyId=REDACTED&aa&bb"),
          arguments(
              "https://service.com?aa&bb&AWSAccessKeyId=AKIAIOSFODNN7",
              "https://service.com?aa&bb&AWSAccessKeyId=REDACTED"),
          arguments(
              "https://service.com?AWSAccessKeyId=AKIAIOSFODNN7&&",
              "https://service.com?AWSAccessKeyId=REDACTED&&"),
          arguments(
              "https://service.com?&&AWSAccessKeyId=AKIAIOSFODNN7",
              "https://service.com?&&AWSAccessKeyId=REDACTED"),
          arguments(
              "https://service.com?AWSAccessKeyId=AKIAIOSFODNN7&a&b#fragment",
              "https://service.com?AWSAccessKeyId=REDACTED&a&b#fragment"));
    }
  }

  @ParameterizedTest
  @ArgumentsSource(ValidRequestMethodsProvider.class)
  void shouldExtractKnownMethods(String requestMethod) {
    Map<String, String> request = new HashMap<>();
    request.put("method", requestMethod);

    AttributesExtractor<Map<String, String>, Map<String, String>> extractor =
        HttpClientAttributesExtractor.create(new TestHttpClientAttributesGetter());

    AttributesBuilder attributes = Attributes.builder();
    extractor.onStart(attributes, Context.root(), request);
    extractor.onEnd(attributes, Context.root(), request, emptyMap(), null);

    assertThat(attributes.build())
        .containsEntry(HTTP_REQUEST_METHOD, requestMethod)
        .doesNotContainKey(HTTP_REQUEST_METHOD_ORIGINAL);
  }

  @ParameterizedTest
  @ValueSource(strings = {"get", "Get"})
  void shouldTreatMethodsAsCaseSensitive(String requestMethod) {
    Map<String, String> request = new HashMap<>();
    request.put("method", requestMethod);

    AttributesExtractor<Map<String, String>, Map<String, String>> extractor =
        HttpClientAttributesExtractor.create(new TestHttpClientAttributesGetter());

    AttributesBuilder attributes = Attributes.builder();
    extractor.onStart(attributes, Context.root(), request);
    extractor.onEnd(attributes, Context.root(), request, emptyMap(), null);

    assertThat(attributes.build())
        .containsEntry(HTTP_REQUEST_METHOD, HttpConstants._OTHER)
        .containsEntry(HTTP_REQUEST_METHOD_ORIGINAL, requestMethod);
  }

  @ParameterizedTest
  @ValueSource(strings = {"PURGE", "not a method really"})
  void shouldUseOtherForUnknownMethods(String requestMethod) {
    Map<String, String> request = new HashMap<>();
    request.put("method", requestMethod);

    AttributesExtractor<Map<String, String>, Map<String, String>> extractor =
        HttpClientAttributesExtractor.create(new TestHttpClientAttributesGetter());

    AttributesBuilder attributes = Attributes.builder();
    extractor.onStart(attributes, Context.root(), request);
    extractor.onEnd(attributes, Context.root(), request, emptyMap(), null);

    assertThat(attributes.build())
        .containsEntry(HTTP_REQUEST_METHOD, HttpConstants._OTHER)
        .containsEntry(HTTP_REQUEST_METHOD_ORIGINAL, requestMethod);
  }

  @ParameterizedTest
  @ValueSource(strings = {"only", "custom", "methods", "allowed"})
  void shouldExtractKnownMethods_override(String requestMethod) {
    Map<String, String> request = new HashMap<>();
    request.put("method", requestMethod);

    AttributesExtractor<Map<String, String>, Map<String, String>> extractor =
        HttpClientAttributesExtractor.builder(new TestHttpClientAttributesGetter())
            .setKnownMethods(new HashSet<>(asList("only", "custom", "methods", "allowed")))
            .build();

    AttributesBuilder attributes = Attributes.builder();
    extractor.onStart(attributes, Context.root(), request);
    extractor.onEnd(attributes, Context.root(), request, emptyMap(), null);

    assertThat(attributes.build())
        .containsEntry(HTTP_REQUEST_METHOD, requestMethod)
        .doesNotContainKey(HTTP_REQUEST_METHOD_ORIGINAL);
  }

  @ParameterizedTest
  @ArgumentsSource(ValidRequestMethodsProvider.class)
  void shouldUseOtherForUnknownMethods_override(String requestMethod) {
    Map<String, String> request = new HashMap<>();
    request.put("method", requestMethod);

    AttributesExtractor<Map<String, String>, Map<String, String>> extractor =
        HttpClientAttributesExtractor.builder(new TestHttpClientAttributesGetter())
            .setKnownMethods(new HashSet<>(asList("only", "custom", "methods", "allowed")))
            .build();

    AttributesBuilder attributes = Attributes.builder();
    extractor.onStart(attributes, Context.root(), request);
    extractor.onEnd(attributes, Context.root(), request, emptyMap(), null);

    assertThat(attributes.build())
        .containsEntry(HTTP_REQUEST_METHOD, HttpConstants._OTHER)
        .containsEntry(HTTP_REQUEST_METHOD_ORIGINAL, requestMethod);
  }

  @Test
  void shouldExtractErrorType_httpStatusCode() {
    Map<String, String> response = new HashMap<>();
    response.put("statusCode", "400");

    AttributesExtractor<Map<String, String>, Map<String, String>> extractor =
        HttpClientAttributesExtractor.create(new TestHttpClientAttributesGetter());

    AttributesBuilder attributes = Attributes.builder();
    extractor.onStart(attributes, Context.root(), emptyMap());
    extractor.onEnd(attributes, Context.root(), emptyMap(), response, null);

    assertThat(attributes.build())
        .containsEntry(HTTP_RESPONSE_STATUS_CODE, 400)
        .containsEntry(ERROR_TYPE, "400");
  }

  @Test
  void shouldExtractErrorType_getter() {
    Map<String, String> request = new HashMap<>();
    request.put("statusCode", "0");
    request.put("errorType", "custom error type");

    AttributesExtractor<Map<String, String>, Map<String, String>> extractor =
        HttpClientAttributesExtractor.create(new TestHttpClientAttributesGetter());

    AttributesBuilder attributes = Attributes.builder();
    extractor.onStart(attributes, Context.root(), emptyMap());
    extractor.onEnd(attributes, Context.root(), request, emptyMap(), null);

    assertThat(attributes.build()).containsEntry(ERROR_TYPE, "custom error type");
  }

  @Test
  void shouldExtractErrorType_exceptionClassName() {
    AttributesExtractor<Map<String, String>, Map<String, String>> extractor =
        HttpClientAttributesExtractor.create(new TestHttpClientAttributesGetter());

    AttributesBuilder attributes = Attributes.builder();
    extractor.onStart(attributes, Context.root(), emptyMap());
    extractor.onEnd(attributes, Context.root(), emptyMap(), emptyMap(), new ConnectException());

    assertThat(attributes.build()).containsEntry(ERROR_TYPE, "java.net.ConnectException");
  }

  @Test
  void shouldExtractErrorType_other() {
    AttributesExtractor<Map<String, String>, Map<String, String>> extractor =
        HttpClientAttributesExtractor.create(new TestHttpClientAttributesGetter());

    AttributesBuilder attributes = Attributes.builder();
    extractor.onStart(attributes, Context.root(), emptyMap());
    extractor.onEnd(attributes, Context.root(), emptyMap(), emptyMap(), null);

    assertThat(attributes.build()).containsEntry(ERROR_TYPE, HttpConstants._OTHER);
  }

  @Test
  void shouldExtractServerAddressAndPortFromHostHeader() {
    Map<String, String> request = new HashMap<>();
    request.put("header.host", "github.com:123");

    Map<String, String> response = new HashMap<>();
    response.put("statusCode", "200");

    AttributesExtractor<Map<String, String>, Map<String, String>> extractor =
        HttpClientAttributesExtractor.create(new TestHttpClientAttributesGetter());

    AttributesBuilder startAttributes = Attributes.builder();
    extractor.onStart(startAttributes, Context.root(), request);
    assertThat(startAttributes.build())
        .containsOnly(entry(SERVER_ADDRESS, "github.com"), entry(SERVER_PORT, 123L));

    AttributesBuilder endAttributes = Attributes.builder();
    extractor.onEnd(endAttributes, Context.root(), request, response, null);
    assertThat(endAttributes.build()).containsOnly(entry(HTTP_RESPONSE_STATUS_CODE, 200L));
  }

  @Test
  void shouldExtractPeerAddressEvenIfItDuplicatesServerAddress() {
    Map<String, String> request = new HashMap<>();
    request.put("networkPeerAddress", "1.2.3.4");
    request.put("networkPeerPort", "456");
    request.put("serverAddress", "1.2.3.4");
    request.put("serverPort", "123");

    Map<String, String> response = new HashMap<>();
    response.put("statusCode", "200");

    AttributesExtractor<Map<String, String>, Map<String, String>> extractor =
        HttpClientAttributesExtractor.create(new TestHttpClientAttributesGetter());

    AttributesBuilder startAttributes = Attributes.builder();
    extractor.onStart(startAttributes, Context.root(), request);
    assertThat(startAttributes.build())
        .containsOnly(entry(SERVER_ADDRESS, "1.2.3.4"), entry(SERVER_PORT, 123L));

    AttributesBuilder endAttributes = Attributes.builder();
    extractor.onEnd(endAttributes, Context.root(), request, response, null);
    assertThat(endAttributes.build())
        .containsOnly(
            entry(HTTP_RESPONSE_STATUS_CODE, 200L),
            entry(NETWORK_PEER_ADDRESS, "1.2.3.4"),
            entry(NETWORK_PEER_PORT, 456L));
  }

  @Test
  void shouldExtractProtocolNameDifferentFromHttp() {
    Map<String, String> request = new HashMap<>();
    request.put("networkProtocolName", "spdy");
    request.put("networkProtocolVersion", "3.1");

    Map<String, String> response = new HashMap<>();
    response.put("statusCode", "200");

    AttributesExtractor<Map<String, String>, Map<String, String>> extractor =
        HttpClientAttributesExtractor.create(new TestHttpClientAttributesGetter());

    AttributesBuilder startAttributes = Attributes.builder();
    extractor.onStart(startAttributes, Context.root(), request);
    assertThat(startAttributes.build()).isEmpty();

    AttributesBuilder endAttributes = Attributes.builder();
    extractor.onEnd(endAttributes, Context.root(), request, response, null);
    assertThat(endAttributes.build())
        .containsOnly(
            entry(HTTP_RESPONSE_STATUS_CODE, 200L),
            entry(NETWORK_PROTOCOL_NAME, "spdy"),
            entry(NETWORK_PROTOCOL_VERSION, "3.1"));
  }
}
