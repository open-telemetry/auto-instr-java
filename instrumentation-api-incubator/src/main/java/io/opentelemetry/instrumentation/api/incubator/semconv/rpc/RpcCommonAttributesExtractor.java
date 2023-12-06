/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.incubator.semconv.rpc;

import static io.opentelemetry.instrumentation.api.internal.AttributesExtractorUtil.internalSet;

import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.semconv.SemanticAttributes;
import javax.annotation.Nullable;

abstract class RpcCommonAttributesExtractor<REQUEST, RESPONSE>
    implements AttributesExtractor<REQUEST, RESPONSE> {

  private final RpcAttributeGetter<REQUEST> getter;

  RpcCommonAttributesExtractor(RpcAttributeGetter<REQUEST> getter) {
    this.getter = getter;
  }

  @Override
  public final void onStart(AttributesBuilder attributes, Context parentContext, REQUEST request) {
    internalSet(attributes, SemanticAttributes.RPC_SYSTEM, getter.getSystem(request));
    internalSet(attributes, SemanticAttributes.RPC_SERVICE, getter.getService(request));
    internalSet(attributes, SemanticAttributes.RPC_METHOD, getter.getMethod(request));
  }

  @Override
  public final void onEnd(
      AttributesBuilder attributes,
      Context context,
      REQUEST request,
      @Nullable RESPONSE response,
      @Nullable Throwable error) {
    // No response attributes
  }
}
