/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.v5_0;

import io.opentelemetry.javaagent.instrumentation.apachehttpclient.commons.OtelHttpRequest;
import java.net.InetSocketAddress;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpRequest;

public final class ApacheHttpClientRequest implements OtelHttpRequest {
  @Nullable private final HttpHost target;
  private final HttpRequest httpRequest;

  public ApacheHttpClientRequest(@Nullable HttpHost target, HttpRequest httpRequest) {
    this.target = target;
    this.httpRequest = httpRequest;
  }

  public ApacheHttpClientRequest(HttpHost target, ClassicHttpRequest httpRequest) {
    this(target, (HttpRequest) new RequestWithHost(target, httpRequest));
  }

  public ApacheHttpClientRequest(HttpRequest httpRequest) {
    this(null, httpRequest);
  }

  @Override
  public String getPeerName() {
    return ApacheHttpClientAttributesHelper.getPeerName(httpRequest);
  }

  @Override
  public Integer getPeerPort() {
    return ApacheHttpClientAttributesHelper.getPeerPort(httpRequest);
  }

  @Override
  public InetSocketAddress getPeerSocketAddress() {
    return ApacheHttpClientAttributesHelper.getPeerSocketAddress(target);
  }

  @Override
  public String getMethod() {
    return httpRequest.getMethod();
  }

  @Override
  public String getUrl() {
    return ApacheHttpClientAttributesHelper.getUrl(httpRequest);
  }

  @Override
  public String getFlavor() {
    return ApacheHttpClientAttributesHelper.getFlavor(httpRequest.getVersion());
  }

  @Override
  public List<String> getHeader(String name) {
    return ApacheHttpClientAttributesHelper.getHeader(httpRequest, name);
  }

  @Override
  public void setHeader(String key, String value) {
    httpRequest.setHeader(key, value);
  }
}
