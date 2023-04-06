/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.commons;

import io.opentelemetry.instrumentation.api.instrumenter.net.InetSocketAddressNetClientAttributesGetter;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.net.InetSocketAddress;
import javax.annotation.Nullable;

public final class ApacheHttpClientNetAttributesGetter
    extends InetSocketAddressNetClientAttributesGetter<OtelHttpRequest, OtelHttpResponse> {
  @Override
  public String getTransport(OtelHttpRequest request, @Nullable OtelHttpResponse response) {
    return SemanticAttributes.NetTransportValues.IP_TCP;
  }

  @Override
  @Nullable
  public String getPeerName(OtelHttpRequest request) {
    return request.getPeerName();
  }

  @Override
  public Integer getPeerPort(OtelHttpRequest request) {
    return request.getPeerPort();
  }

  @Nullable
  @Override
  protected InetSocketAddress getPeerSocketAddress(
      OtelHttpRequest request, @Nullable OtelHttpResponse otelHttpResponse) {
    return request.getPeerSocketAddress();
  }
}
