/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.netty.common.v4_0.internal.client;

import io.netty.channel.Channel;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientAttributesGetter;
import io.opentelemetry.instrumentation.netty.common.internal.NettyConnectionRequest;
import io.opentelemetry.instrumentation.netty.common.v4_0.internal.ChannelUtil;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

enum NettyConnectHttpAttributesGetter
    implements HttpClientAttributesGetter<NettyConnectionRequest, Channel> {
  INSTANCE;

  @Nullable
  @Override
  public String getUrlFull(NettyConnectionRequest nettyConnectionRequest) {
    return null;
  }

  @Nullable
  @Override
  public String getHttpRequestMethod(NettyConnectionRequest nettyConnectionRequest) {
    return null;
  }

  @Override
  public List<String> getHttpRequestHeader(
      NettyConnectionRequest nettyConnectionRequest, String name) {
    return Collections.emptyList();
  }

  @Nullable
  @Override
  public Integer getHttpResponseStatusCode(
      NettyConnectionRequest nettyConnectionRequest, Channel channel, @Nullable Throwable error) {
    return null;
  }

  @Override
  public List<String> getHttpResponseHeader(
      NettyConnectionRequest nettyConnectionRequest, Channel channel, String name) {
    return Collections.emptyList();
  }

  @Override
  public String getNetworkTransport(NettyConnectionRequest request, @Nullable Channel channel) {
    return ChannelUtil.getNetworkTransport(channel);
  }

  @Nullable
  @Override
  public String getServerAddress(NettyConnectionRequest request) {
    SocketAddress requestedAddress = request.remoteAddressOnStart();
    if (requestedAddress instanceof InetSocketAddress) {
      return ((InetSocketAddress) requestedAddress).getHostString();
    }
    return null;
  }

  @Nullable
  @Override
  public Integer getServerPort(NettyConnectionRequest request) {
    SocketAddress requestedAddress = request.remoteAddressOnStart();
    if (requestedAddress instanceof InetSocketAddress) {
      return ((InetSocketAddress) requestedAddress).getPort();
    }
    return null;
  }

  @Nullable
  @Override
  public InetSocketAddress getNetworkPeerInetSocketAddress(
      NettyConnectionRequest request, @Nullable Channel channel) {
    if (channel == null) {
      return null;
    }
    SocketAddress remoteAddress = channel.remoteAddress();
    if (remoteAddress instanceof InetSocketAddress) {
      return (InetSocketAddress) remoteAddress;
    }
    return null;
  }
}
