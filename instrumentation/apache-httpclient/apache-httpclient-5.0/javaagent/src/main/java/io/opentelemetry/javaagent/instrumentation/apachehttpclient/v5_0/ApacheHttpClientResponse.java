/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.v5_0;

import io.opentelemetry.javaagent.instrumentation.apachehttpclient.commons.OtelHttpResponse;
import java.util.List;
import org.apache.hc.core5.http.HttpResponse;

public final class ApacheHttpClientResponse implements OtelHttpResponse {
  private final HttpResponse httpResponse;

  public ApacheHttpClientResponse(HttpResponse httpResponse) {
    this.httpResponse = httpResponse;
  }

  @Override
  public Integer statusCode() {
    return httpResponse.getCode();
  }

  @Override
  public String getFlavour() {
    return ApacheHttpClientAttributesHelper.getFlavor(httpResponse.getVersion());
  }

  @Override
  public List<String> getHeader(String name) {
    return ApacheHttpClientAttributesHelper.getHeader(httpResponse, name);
  }

  @Override
  public String getFirstHeader(String name) {
    return ApacheHttpClientAttributesHelper.getFirstHeader(httpResponse, name);
  }
}
