/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.thrift;

import io.opentelemetry.instrumentation.api.instrumenter.rpc.RpcAttributesGetter;
import javax.annotation.Nullable;

enum ThriftRpcAttributesGetter implements RpcAttributesGetter<ThriftRequest> {
  INSTANCE;

  @Override
  public String getSystem(ThriftRequest request) {
    return "thrift";
  }

  @Override
  @Nullable
  public String getService(ThriftRequest request) {
    return "thrift_Service";
  }

  @Override
  @Nullable
  public String getMethod(ThriftRequest request) {
    return request.getMethodName();
  }
}
