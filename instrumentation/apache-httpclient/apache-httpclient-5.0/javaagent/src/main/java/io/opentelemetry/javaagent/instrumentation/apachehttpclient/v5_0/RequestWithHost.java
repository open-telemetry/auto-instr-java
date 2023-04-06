/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.v5_0;

import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.message.HttpRequestWrapper;
import org.apache.hc.core5.net.URIAuthority;

public final class RequestWithHost extends HttpRequestWrapper implements ClassicHttpRequest {
  private final String scheme;
  private final URIAuthority authority;
  private final ClassicHttpRequest httpRequest;

  public RequestWithHost(HttpHost httpHost, ClassicHttpRequest httpRequest) {
    super(httpRequest);

    this.scheme = httpHost.getSchemeName();
    this.authority = new URIAuthority(httpHost.getHostName(), httpHost.getPort());
    this.httpRequest = httpRequest;
  }

  @Override
  public String getScheme() {
    return scheme;
  }

  @Override
  public URIAuthority getAuthority() {
    return authority;
  }

  @Override
  public HttpEntity getEntity() {
    return httpRequest.getEntity();
  }

  @Override
  public void setEntity(HttpEntity httpEntity) {
    httpRequest.setEntity(httpEntity);
  }
}
