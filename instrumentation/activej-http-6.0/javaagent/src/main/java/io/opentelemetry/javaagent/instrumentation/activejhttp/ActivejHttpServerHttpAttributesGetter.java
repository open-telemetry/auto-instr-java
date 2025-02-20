/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.activejhttp;

import io.activej.http.HttpRequest;
import io.activej.http.HttpResponse;
import io.activej.http.UrlParser;
import io.opentelemetry.instrumentation.api.semconv.http.HttpServerAttributesGetter;
import java.util.List;
import javax.annotation.Nullable;

final class ActivejHttpServerHttpAttributesGetter
    implements HttpServerAttributesGetter<HttpRequest, HttpResponse> {

  @Override
  public String getHttpRequestMethod(HttpRequest request) {
    return ActivejHttpServerUtil.getHttpRequestMethod(request);
  }

  @Override
  public List<String> getHttpRequestHeader(HttpRequest request, String name) {
    return ActivejHttpServerUtil.requestHeader(request, name);
  }

  @Override
  public Integer getHttpResponseStatusCode(
      HttpRequest request, HttpResponse httpResponse, @Nullable Throwable error) {
    return ActivejHttpServerUtil.getHttpResponseStatusCode(request, httpResponse, error);
  }

  @Override
  public List<String> getHttpResponseHeader(
      HttpRequest request, HttpResponse httpResponse, String name) {
    return ActivejHttpServerUtil.getHttpResponseHeader(request, httpResponse, name);
  }

  @Override
  public String getUrlScheme(HttpRequest request) {
    return UrlParser.of(request.getFullUrl()).getProtocol().name();
  }

  @Override
  public String getUrlPath(HttpRequest request) {
    return request.getPath();
  }

  @Override
  public String getUrlQuery(HttpRequest request) {
    return request.getQuery();
  }

  @Override
  public String getNetworkProtocolName(HttpRequest request, @Nullable HttpResponse httpResponse) {
    return ActivejHttpServerUtil.getNetworkProtocolName(request);
  }

  @Override
  public String getNetworkProtocolVersion(
      HttpRequest request, @Nullable HttpResponse httpResponse) {
    return ActivejHttpServerUtil.getNetworkProtocolVersion(request.getVersion());
  }
}
