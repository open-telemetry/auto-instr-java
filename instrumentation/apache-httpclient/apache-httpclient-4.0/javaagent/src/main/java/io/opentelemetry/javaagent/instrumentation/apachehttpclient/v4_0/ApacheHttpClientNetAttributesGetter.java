/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.v4_0;

import io.opentelemetry.instrumentation.api.instrumenter.net.NetClientAttributesGetter;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import javax.annotation.Nullable;
import org.apache.http.HttpResponse;

final class ApacheHttpClientNetAttributesGetter
    implements NetClientAttributesGetter<ApacheHttpClientRequest, HttpResponse> {

  @Override
  public String transport(ApacheHttpClientRequest request, @Nullable HttpResponse response) {
    return SemanticAttributes.NetTransportValues.IP_TCP;
  }

  @Override
  @Nullable
  public String peerName(ApacheHttpClientRequest request, @Nullable HttpResponse response) {
    return request.getPeerName();
  }

  @Override
  public Integer peerPort(ApacheHttpClientRequest request, @Nullable HttpResponse response) {
    return request.getPeerPort();
  }

  @Nullable
  @Override
  public String sockFamily(ApacheHttpClientRequest request, @Nullable HttpResponse response) {
    return null;
  }

  @Nullable
  @Override
  public String sockPeerAddr(ApacheHttpClientRequest request, @Nullable HttpResponse response) {
    return null;
  }

  @Nullable
  @Override
  public String sockPeerName(ApacheHttpClientRequest request, @Nullable HttpResponse response) {
    return null;
  }

  @Nullable
  @Override
  public Integer sockPeerPort(ApacheHttpClientRequest request, @Nullable HttpResponse response) {
    return null;
  }
}
