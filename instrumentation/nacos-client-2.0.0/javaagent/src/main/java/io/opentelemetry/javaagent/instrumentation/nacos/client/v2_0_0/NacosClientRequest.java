/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.nacos.client.v2_0_0;

import com.alibaba.nacos.api.remote.request.Request;
import io.opentelemetry.api.common.Attributes;
import javax.annotation.Nonnull;

public final class NacosClientRequest {
  private final String methodName;
  private final Class<?> declaringClass;
  private final String spanName;
  private final Attributes attributes;

  public NacosClientRequest(
      String methodName, Class<?> declaringClass, String spanName, Attributes attributes) {
    this.methodName = methodName;
    this.declaringClass = declaringClass;
    this.spanName = spanName;
    this.attributes = attributes;
  }

  @Nonnull
  public static NacosClientRequest createRequest(
      @Nonnull String methodName, @Nonnull Class<?> declaringClass, @Nonnull Request request) {
    NacosClientRequestOperator operator = NacosClientHelper.getOperator(request);
    String spanName = operator.getName(request);
    Attributes attributes = operator.getAttributes(request);
    return new NacosClientRequest(methodName, declaringClass, spanName, attributes);
  }

  public String getMethodName() {
    return methodName;
  }

  public Class<?> getDeclaringClass() {
    return declaringClass;
  }

  public String getSpanName() {
    return spanName;
  }

  public Attributes getAttributes() {
    return attributes;
  }
}
