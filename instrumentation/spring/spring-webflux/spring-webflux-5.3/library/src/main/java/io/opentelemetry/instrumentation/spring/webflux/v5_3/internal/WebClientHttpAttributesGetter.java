/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.spring.webflux.v5_3.internal;

import static java.util.Collections.emptyList;

import io.opentelemetry.instrumentation.api.semconv.http.HttpClientAttributesGetter;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public enum WebClientHttpAttributesGetter
    implements HttpClientAttributesGetter<ClientRequest, ClientResponse> {
  INSTANCE;

  private static final String URI_TEMPLATE_ATTRIBUTE = WebClient.class.getName() + ".uriTemplate";
  private static final Pattern PATTERN_BEFORE_PATH = Pattern.compile("^https?://[^/]+/");

  @Override
  public String getUrlFull(ClientRequest request) {
    return request.url().toString();
  }

  @Nullable
  @Override
  public String getHttpRoute(ClientRequest clientRequest) {
    Map<String, Object> attributes = clientRequest.attributes();
    Object value = attributes.get(URI_TEMPLATE_ATTRIBUTE);
    if (value instanceof String) {
      String uriTemplate = (String) value;
      String path = PATTERN_BEFORE_PATH.matcher(uriTemplate).replaceFirst("");
      return path.startsWith("/") ? path : "/" + path;
    }
    return null;
  }

  @Override
  public String getHttpRequestMethod(ClientRequest request) {
    return request.method().name();
  }

  @Override
  public List<String> getHttpRequestHeader(ClientRequest request, String name) {
    return request.headers().getOrDefault(name, emptyList());
  }

  @Override
  @Nullable
  public Integer getHttpResponseStatusCode(
      ClientRequest request, ClientResponse response, @Nullable Throwable error) {
    return StatusCodes.get(response);
  }

  @Override
  public List<String> getHttpResponseHeader(
      ClientRequest request, ClientResponse response, String name) {
    return response.headers().header(name);
  }

  @Nullable
  @Override
  public String getServerAddress(ClientRequest request) {
    return request.url().getHost();
  }

  @Override
  public Integer getServerPort(ClientRequest request) {
    return request.url().getPort();
  }

  @Nullable
  @Override
  public String getErrorType(
      ClientRequest request, @Nullable ClientResponse response, @Nullable Throwable error) {
    // if both response and error are null it means the request has been cancelled -- see the
    // WebClientTracingFilter class
    if (response == null && error == null) {
      return "cancelled";
    }
    return null;
  }
}
