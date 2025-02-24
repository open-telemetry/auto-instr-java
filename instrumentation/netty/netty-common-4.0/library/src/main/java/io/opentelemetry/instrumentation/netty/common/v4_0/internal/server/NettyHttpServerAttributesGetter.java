/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.netty.common.v4_0.internal.server;

import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpVersion;
import io.opentelemetry.instrumentation.api.semconv.http.HttpServerAttributesGetter;
import io.opentelemetry.instrumentation.netty.common.v4_0.HttpRequestAndChannel;
import io.opentelemetry.instrumentation.netty.common.v4_0.internal.ChannelUtil;
import io.opentelemetry.instrumentation.netty.common.v4_0.internal.HttpSchemeUtil;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import javax.annotation.Nullable;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class NettyHttpServerAttributesGetter
    implements HttpServerAttributesGetter<HttpRequestAndChannel, HttpResponse> {

  @Override
  public String getHttpRequestMethod(HttpRequestAndChannel requestAndChannel) {
    return requestAndChannel.request().getMethod().name();
  }

  @Override
  public List<String> getHttpRequestHeader(HttpRequestAndChannel requestAndChannel, String name) {
    return requestAndChannel.request().headers().getAll(name);
  }

  @Override
  public Integer getHttpResponseStatusCode(
      HttpRequestAndChannel requestAndChannel, HttpResponse response, @Nullable Throwable error) {
    return response.getStatus().code();
  }

  @Override
  public List<String> getHttpResponseHeader(
      HttpRequestAndChannel requestAndChannel, HttpResponse response, String name) {
    return response.headers().getAll(name);
  }

  @Override
  public String getUrlScheme(HttpRequestAndChannel requestAndChannel) {
    return HttpSchemeUtil.getScheme(requestAndChannel);
  }

  @Override
  public String getUrlPath(HttpRequestAndChannel requestAndChannel) {
    String fullPath = requestAndChannel.request().getUri();
    int separatorPos = fullPath.indexOf('?');
    return separatorPos == -1 ? fullPath : fullPath.substring(0, separatorPos);
  }

  @Override
  public String getUrlQuery(HttpRequestAndChannel requestAndChannel) {
    String fullPath = requestAndChannel.request().getUri();
    int separatorPos = fullPath.indexOf('?');
    return separatorPos == -1 ? null : fullPath.substring(separatorPos + 1);
  }

  @Override
  public String getNetworkTransport(
      HttpRequestAndChannel requestAndChannel, HttpResponse response) {
    return ChannelUtil.getNetworkTransport(requestAndChannel.channel());
  }

  @Override
  public String getNetworkProtocolName(
      HttpRequestAndChannel requestAndChannel, @Nullable HttpResponse response) {
    return requestAndChannel.request().getProtocolVersion().protocolName();
  }

  @Override
  public String getNetworkProtocolVersion(
      HttpRequestAndChannel requestAndChannel, @Nullable HttpResponse response) {
    HttpVersion version = requestAndChannel.request().getProtocolVersion();
    if (version.minorVersion() == 0) {
      return Integer.toString(version.majorVersion());
    }
    return version.majorVersion() + "." + version.minorVersion();
  }

  @Override
  @Nullable
  public InetSocketAddress getNetworkPeerInetSocketAddress(
      HttpRequestAndChannel requestAndChannel, @Nullable HttpResponse response) {
    SocketAddress address = requestAndChannel.remoteAddress();
    if (address instanceof InetSocketAddress) {
      return (InetSocketAddress) address;
    }
    return null;
  }

  @Nullable
  @Override
  public InetSocketAddress getNetworkLocalInetSocketAddress(
      HttpRequestAndChannel requestAndChannel, @Nullable HttpResponse response) {
    SocketAddress address = requestAndChannel.channel().localAddress();
    if (address instanceof InetSocketAddress) {
      return (InetSocketAddress) address;
    }
    return null;
  }
}
