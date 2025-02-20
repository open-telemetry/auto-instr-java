/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.activejhttp;

import static io.activej.http.HttpError.internalServerError500;
import static java.util.Collections.emptyList;

import io.activej.http.HttpHeaders;
import io.activej.http.HttpRequest;
import io.activej.http.HttpResponse;
import io.activej.http.HttpVersion;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

public final class ActivejHttpServerUtil {

  private ActivejHttpServerUtil() {
    throw new UnsupportedOperationException();
  }

  static String getHttpRequestMethod(HttpRequest request) {
    return request.getMethod().name();
  }

  static List<String> requestHeader(HttpRequest request, String name) {
    String headerValue = request.getHeader(HttpHeaders.of(name));
    if (headerValue == null) {
      return emptyList();
    }
    return Arrays.stream(headerValue.split(",")).collect(Collectors.toList());
  }

  static Integer getHttpResponseStatusCode(
      HttpRequest request, HttpResponse httpResponse, @Nullable Throwable error) {
    if (error != null && httpResponse.getCode() <= 0) {
      return internalServerError500().getCode();
    }
    return httpResponse.getCode();
  }

  static List<String> getHttpResponseHeader(
      HttpRequest request, HttpResponse httpResponse, String name) {
    String headerValue = httpResponse.getHeader(HttpHeaders.of(name));
    if (headerValue == null) {
      return emptyList();
    }
    return Arrays.stream(headerValue.split(",")).toList();
  }

  static String getNetworkProtocolVersion(HttpVersion version) {
    switch (version) {
      case HTTP_0_9:
        return "0.9";
      case HTTP_1_0:
        return "1.0";
      case HTTP_1_1:
        return "1.1";
      case HTTP_2_0:
        return "2.0";
    }
    return "unknown";
  }

  static String getNetworkProtocolName(HttpRequest request) {
    if (request.getVersion() == HttpVersion.HTTP_0_9
        || request.getVersion() == HttpVersion.HTTP_1_0
        || request.getVersion() == HttpVersion.HTTP_1_1
        || request.getVersion() == HttpVersion.HTTP_2_0) {
      return "http";
    }
    return null;
  }
}
