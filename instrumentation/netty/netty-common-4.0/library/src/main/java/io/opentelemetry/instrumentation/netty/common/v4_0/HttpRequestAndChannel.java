/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.netty.common.v4_0;

import com.google.auto.value.AutoValue;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.HttpRequest;
import java.net.SocketAddress;
import javax.annotation.Nullable;

/** A tuple of an {@link HttpRequest} and a {@link Channel}. */
@AutoValue
public abstract class HttpRequestAndChannel {

  /** Create a new {@link HttpRequestAndChannel}. */
  public static HttpRequestAndChannel create(HttpRequest request, Channel channel) {
    return new AutoValue_HttpRequestAndChannel(request, channel, channel.remoteAddress());
  }

  /** Returns the {@link HttpRequest}. */
  public abstract HttpRequest request();

  /** Returns the {@link Channel}. */
  public abstract Channel channel();

  /**
   * Return the {@link Channel#remoteAddress()} present when this {@link HttpRequestAndChannel} was
   * created.
   *
   * <p>We capture the remote address early because netty may return null when calling {@link
   * Channel#remoteAddress()} at the end of processing in cases of timeouts or other connection
   * issues.
   */
  @Nullable
  public abstract SocketAddress remoteAddress();
}
