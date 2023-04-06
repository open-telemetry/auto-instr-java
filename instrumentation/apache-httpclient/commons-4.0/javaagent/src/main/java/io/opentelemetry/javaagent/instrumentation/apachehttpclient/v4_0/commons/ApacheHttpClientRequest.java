/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.v4_0.commons;

import static io.opentelemetry.javaagent.instrumentation.apachehttpclient.v4_0.commons.ApacheHttpClientAttributesHelper.getUri;

import io.opentelemetry.javaagent.instrumentation.apachehttpclient.commons.OtelHttpRequest;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;

public class ApacheHttpClientRequest implements OtelHttpRequest {
  @Nullable private final URI uri;
  private final HttpRequest httpRequest;

  public ApacheHttpClientRequest(URI uri, HttpRequest httpRequest) {
    this.uri = uri;
    this.httpRequest = httpRequest;
  }

  public ApacheHttpClientRequest(HttpHost target, HttpRequest httpRequest) {
    this(getUri(target, httpRequest), httpRequest);
  }

  @Override
  public String getMethod() {
    return httpRequest.getRequestLine().getMethod();
  }

  @Override
  public String getUrl() {
    return uri == null ? null : uri.toString();
  }

  @Override
  public String getFlavor() {
    return ApacheHttpClientAttributesHelper.getFlavor(httpRequest.getProtocolVersion());
  }

  @Override
  @Nullable
  public String getPeerName() {
    return ApacheHttpClientAttributesHelper.getPeerName(uri);
  }

  @Override
  @Nullable
  public Integer getPeerPort() {
    return ApacheHttpClientAttributesHelper.getPeerPort(uri);
  }

  @Override
  public InetSocketAddress getPeerSocketAddress() {
    return null;
  }

  @Override
  public List<String> getHeader(String name) {
    return ApacheHttpClientAttributesHelper.getHeader(httpRequest, name);
  }

  @Override
  public void setHeader(String name, String value) {
    httpRequest.setHeader(name, value);
  }
}
