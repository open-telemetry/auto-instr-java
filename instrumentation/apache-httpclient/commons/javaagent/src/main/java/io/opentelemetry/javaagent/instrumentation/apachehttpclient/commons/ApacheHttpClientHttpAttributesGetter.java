/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.commons;

import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesGetter;
import java.util.List;
import javax.annotation.Nullable;

public final class ApacheHttpClientHttpAttributesGetter
    implements HttpClientAttributesGetter<OtelHttpRequest, OtelHttpResponse> {
  @Override
  public String getMethod(OtelHttpRequest request) {
    return request.getMethod();
  }

  @Override
  public String getUrl(OtelHttpRequest request) {
    return request.getUrl();
  }

  @Override
  public List<String> getRequestHeader(OtelHttpRequest request, String name) {
    return request.getHeader(name);
  }

  @Override
  public Integer getStatusCode(
      OtelHttpRequest request, OtelHttpResponse response, @Nullable Throwable error) {
    return response.statusCode();
  }

  @Override
  @Nullable
  public String getFlavor(OtelHttpRequest request, @Nullable OtelHttpResponse response) {
    String flavor = request.getFlavor();
    if (flavor == null && response != null) {
      String responseFlavour = response.getFlavour();
      if (responseFlavour != null) {
        flavor = responseFlavour;
      }
    }
    return flavor;
  }

  @Override
  public List<String> getResponseHeader(
      OtelHttpRequest request, OtelHttpResponse response, String name) {
    return response.getHeader(name);
  }
}
