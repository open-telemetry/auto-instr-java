/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.nacos.client.v2_0_0.extractors;

import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.opentelemetry.javaagent.instrumentation.nacos.client.v2_0_0.NacosClientRequest;

public class NacosClientSpanNameExtractor implements SpanNameExtractor<NacosClientRequest> {
  @Override
  public String extract(NacosClientRequest nacosClientRequest) {
    return nacosClientRequest.getSpanName();
  }
}
