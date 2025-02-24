/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.netty.common.v4_0.internal;

import io.netty.channel.ChannelHandler;
import io.opentelemetry.instrumentation.netty.common.v4_0.HttpRequestAndChannel;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class HttpSchemeUtil {

  private static final Class<? extends ChannelHandler> sslHandlerClass = getSslHandlerClass();

  @SuppressWarnings("unchecked")
  private static Class<? extends ChannelHandler> getSslHandlerClass() {
    try {
      return (Class<? extends ChannelHandler>)
          Class.forName(
              "io.netty.handler.ssl.SslHandler", false, HttpSchemeUtil.class.getClassLoader());
    } catch (ClassNotFoundException exception) {
      return null;
    }
  }

  public static String getScheme(HttpRequestAndChannel requestAndChannel) {
    return isHttps(requestAndChannel) ? "https" : "http";
  }

  private static boolean isHttps(HttpRequestAndChannel requestAndChannel) {
    return sslHandlerClass != null
        && requestAndChannel.channel().pipeline().get(sslHandlerClass) != null;
  }

  private HttpSchemeUtil() {}
}
