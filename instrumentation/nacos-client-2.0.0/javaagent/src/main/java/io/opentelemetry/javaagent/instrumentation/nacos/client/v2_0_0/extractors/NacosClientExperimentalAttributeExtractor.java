/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.nacos.client.v2_0_0.extractors;

import com.alibaba.nacos.api.remote.response.Response;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.javaagent.instrumentation.nacos.client.v2_0_0.NacosClientRequest;
import javax.annotation.Nullable;

public class NacosClientExperimentalAttributeExtractor
    implements AttributesExtractor<NacosClientRequest, Response> {
  @Override
  public void onStart(
      AttributesBuilder attributes, Context parentContext, NacosClientRequest nacosClientRequest) {
    attributes.putAll(nacosClientRequest.getAttributes());
  }

  @Override
  public void onEnd(
      AttributesBuilder attributes,
      Context context,
      NacosClientRequest nacosClientRequest,
      @Nullable Response response,
      @Nullable Throwable error) {}
}
