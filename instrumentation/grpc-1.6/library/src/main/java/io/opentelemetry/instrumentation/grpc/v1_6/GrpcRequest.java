/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.grpc.v1_6;

import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import java.net.SocketAddress;
import javax.annotation.Nullable;

public final class GrpcRequest {

  private final MethodDescriptor<?, ?> method;

  @Nullable private volatile Metadata metadata;

  @Nullable private volatile String logicalHost;
  private volatile int logicalPort = -1;
  @Nullable private volatile SocketAddress peerSocketAddress;

  private Long requestSize;
  private Long responseSize;

  GrpcRequest(
      MethodDescriptor<?, ?> method,
      @Nullable Metadata metadata,
      @Nullable SocketAddress peerSocketAddress,
      @Nullable String authority) {
    this.method = method;
    this.metadata = metadata;
    this.peerSocketAddress = peerSocketAddress;
    setLogicalAddress(authority);
  }

  private void setLogicalAddress(@Nullable String authority) {
    if (authority == null) {
      return;
    }
    int index = authority.indexOf(':');
    if (index == -1) {
      logicalHost = authority;
    } else {
      logicalHost = authority.substring(0, index);
      try {
        logicalPort = Integer.parseInt(authority.substring(index + 1));
      } catch (NumberFormatException e) {
        // ignore
      }
    }
  }

  public MethodDescriptor<?, ?> getMethod() {
    return method;
  }

  @Nullable
  public Metadata getMetadata() {
    return metadata;
  }

  void setMetadata(Metadata metadata) {
    this.metadata = metadata;
  }

  @Nullable
  public String getLogicalHost() {
    return logicalHost;
  }

  public int getLogicalPort() {
    return logicalPort;
  }

  @Nullable
  public SocketAddress getPeerSocketAddress() {
    return peerSocketAddress;
  }

  void setPeerSocketAddress(SocketAddress peerSocketAddress) {
    this.peerSocketAddress = peerSocketAddress;
  }

  public Long getRequestSize() {
    return requestSize;
  }

  public void setRequestSize(Long requestSize) {
    this.requestSize = requestSize;
  }

  public Long getResponseSize() {
    return responseSize;
  }

  public void setResponseSize(Long responseSize) {
    this.responseSize = responseSize;
  }
}
