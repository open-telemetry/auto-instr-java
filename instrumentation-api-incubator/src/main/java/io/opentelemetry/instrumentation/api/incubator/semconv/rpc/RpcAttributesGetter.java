/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.incubator.semconv.rpc;

import javax.annotation.Nullable;

/**
 * An interface for getting RPC attributes.
 *
 * <p>Instrumentation authors will create implementations of this interface for their specific
 * library/framework. It will be used by the {@link RpcClientAttributesExtractor} or {@link
 * RpcServerAttributesExtractor} to obtain the various RPC attributes in a type-generic way.
 */
public interface RpcAttributesGetter<REQUEST> {

  @Nullable
  String getSystem(REQUEST request);

  @Nullable
  String getService(REQUEST request);

  @Nullable
  String getMethod(REQUEST request);

  default Long getRequestSize(REQUEST request) {
    return null;
  }

  default Long getResponseSize(REQUEST request) {
    return null;
  }
}
