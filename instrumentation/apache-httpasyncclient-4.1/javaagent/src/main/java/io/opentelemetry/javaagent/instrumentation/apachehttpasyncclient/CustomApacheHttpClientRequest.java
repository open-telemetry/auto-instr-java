/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpasyncclient;

import io.opentelemetry.javaagent.instrumentation.apachehttpclient.v4_0.commons.ApacheHttpClientRequest;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import javax.annotation.Nullable;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;

public class CustomApacheHttpClientRequest extends ApacheHttpClientRequest {
  @Nullable private final HttpHost target;

  public CustomApacheHttpClientRequest(HttpHost target, HttpRequest httpRequest) {
    super(target, httpRequest);
    this.target = target;
  }

  @Nullable
  @Override
  public InetSocketAddress getPeerSocketAddress() {
    if (target == null) {
      return null;
    }
    InetAddress inetAddress = target.getAddress();
    return inetAddress == null ? null : new InetSocketAddress(inetAddress, target.getPort());
  }
}
