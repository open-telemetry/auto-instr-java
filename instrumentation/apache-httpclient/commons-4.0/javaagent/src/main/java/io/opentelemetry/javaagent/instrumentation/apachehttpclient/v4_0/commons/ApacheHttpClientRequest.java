/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.v4_0.commons;

import static io.opentelemetry.javaagent.instrumentation.apachehttpclient.v4_0.commons.ApacheHttpClientAttributesHelper.getUri;

import io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.instrumentation.apachehttpclient.commons.BytesTransferMetrics;
import io.opentelemetry.javaagent.instrumentation.apachehttpclient.commons.OtelHttpRequest;
import java.net.URI;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;

public final class ApacheHttpClientRequest implements OtelHttpRequest {
  private final Context parentContext;
  @Nullable private final URI uri;
  private final HttpRequest httpRequest;

  public ApacheHttpClientRequest(Context parentContext, URI uri, HttpRequest httpRequest) {
    this.parentContext = parentContext;
    this.uri = uri;
    this.httpRequest = httpRequest;
  }

  public ApacheHttpClientRequest(Context parentContext, HttpHost target, HttpRequest httpRequest) {
    this(parentContext, getUri(target, httpRequest), httpRequest);
  }

  public ApacheHttpClientRequest withHttpRequest(HttpRequest httpRequest) {
    return new ApacheHttpClientRequest(parentContext, uri, httpRequest);
  }

  @Override
  public BytesTransferMetrics getBytesTransferMetrics() {
    return BytesTransferMetrics.getBytesTransferMetrics(parentContext);
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
  public List<String> getHeader(String name) {
    return ApacheHttpClientAttributesHelper.getHeader(httpRequest, name);
  }

  @Override
  public String getFirstHeader(String name) {
    return ApacheHttpClientAttributesHelper.getFirstHeader(httpRequest, name);
  }

  @Override
  public void setHeader(String name, String value) {
    httpRequest.setHeader(name, value);
  }
}
