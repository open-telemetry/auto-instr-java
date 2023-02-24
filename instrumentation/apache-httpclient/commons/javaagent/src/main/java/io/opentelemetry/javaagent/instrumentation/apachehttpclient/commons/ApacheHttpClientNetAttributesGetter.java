/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.commons;

import io.opentelemetry.instrumentation.api.instrumenter.net.NetClientAttributesGetter;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import javax.annotation.Nullable;

public final class ApacheHttpClientNetAttributesGetter
    implements NetClientAttributesGetter<OtelHttpRequest, OtelHttpResponse> {
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
}
