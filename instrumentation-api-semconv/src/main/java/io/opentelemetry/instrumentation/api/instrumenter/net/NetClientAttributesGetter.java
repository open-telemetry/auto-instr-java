/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.instrumenter.net;

import io.opentelemetry.instrumentation.api.instrumenter.net.internal.InetSocketAddressUtil;
import io.opentelemetry.instrumentation.api.instrumenter.network.NetworkAttributesGetter;
import io.opentelemetry.instrumentation.api.instrumenter.network.ServerAttributesGetter;
import java.net.InetSocketAddress;
import javax.annotation.Nullable;

/**
 * An interface for getting client-based network attributes. It adapts from a type-specific request
 * and response into the 4 common network attribute values.
 *
 * <p>Instrumentation authors will create implementations of this interface for their specific
 * library/framework. It will be used by the NetClientAttributesExtractor to obtain the various
 * network attributes in a type-generic way.
 */
public interface NetClientAttributesGetter<REQUEST, RESPONSE>
    extends NetworkAttributesGetter<REQUEST, RESPONSE>, ServerAttributesGetter<REQUEST, RESPONSE> {

  @Nullable
  default String getTransport(REQUEST request, @Nullable RESPONSE response) {
    return null;
  }

  /**
   * Returns the application protocol used.
   *
   * <p>Examples: `amqp`, `http`, `mqtt`.
   *
   * @deprecated This method is deprecated and will be removed in the following release. Implement
   *     {@link #getNetworkProtocolName(Object, Object)} instead.
   */
  @Deprecated
  @Nullable
  default String getProtocolName(REQUEST request, @Nullable RESPONSE response) {
    return null;
  }

  /**
   * Returns the version of the application protocol used.
   *
   * <p>Examples: `3.1.1`.
   *
   * @deprecated This method is deprecated and will be removed in the following release. Implement
   *     {@link #getNetworkProtocolVersion(Object, Object)} instead.
   */
  @Deprecated
  @Nullable
  default String getProtocolVersion(REQUEST request, @Nullable RESPONSE response) {
    return null;
  }

  /** {@inheritDoc} */
  @Nullable
  @Override
  default String getNetworkType(REQUEST request, @Nullable RESPONSE response) {
    return InetSocketAddressUtil.getNetworkType(getPeerSocketAddress(request, response), null);
  }

  /** {@inheritDoc} */
  @Nullable
  @Override
  default String getNetworkProtocolName(REQUEST request, @Nullable RESPONSE response) {
    return getProtocolName(request, response);
  }

  /** {@inheritDoc} */
  @Nullable
  @Override
  default String getNetworkProtocolVersion(REQUEST request, @Nullable RESPONSE response) {
    return getProtocolVersion(request, response);
  }

  /**
   * Returns the logical peer name.
   *
   * @deprecated This method is deprecated and will be removed in the following release. Implement
   *     {@link #getServerAddress(Object)} instead.
   */
  @Deprecated
  @Nullable
  default String getPeerName(REQUEST request) {
    return null;
  }

  /**
   * Returns the logical peer port.
   *
   * @deprecated This method is deprecated and will be removed in the following release. Implement
   *     {@link #getServerPort(Object)} instead.
   */
  @Deprecated
  @Nullable
  default Integer getPeerPort(REQUEST request) {
    return null;
  }

  /** {@inheritDoc} */
  @Nullable
  @Override
  default String getServerAddress(REQUEST request) {
    return getPeerName(request);
  }

  /** {@inheritDoc} */
  @Nullable
  @Override
  default Integer getServerPort(REQUEST request) {
    return getPeerPort(request);
  }

  /**
   * Returns an {@link InetSocketAddress} object representing the peer socket address.
   *
   * <p>Implementing this method is equivalent to implementing all four of {@link
   * #getSockFamily(Object, Object)}, {@link #getSockPeerAddr(Object, Object)}, {@link
   * #getSockPeerName(Object, Object)} and {@link #getSockPeerPort(Object, Object)}.
   *
   * @deprecated This method is deprecated and will be removed in the following release. Implement
   *     {@link #getServerInetSocketAddress(Object, Object)} instead.
   */
  @Deprecated
  @Nullable
  default InetSocketAddress getPeerSocketAddress(REQUEST request, @Nullable RESPONSE response) {
    return null;
  }

  /** {@inheritDoc} */
  @Nullable
  @Override
  default InetSocketAddress getServerInetSocketAddress(
      REQUEST request, @Nullable RESPONSE response) {
    return getPeerSocketAddress(request, response);
  }

  /**
   * Returns the protocol <a
   * href="https://man7.org/linux/man-pages/man7/address_families.7.html">address family</a> which
   * is used for communication.
   *
   * <p>Examples: {@code inet}, {@code inet6}
   *
   * <p>By default, this method attempts to retrieve the address family using the {@link
   * #getPeerSocketAddress(Object, Object)} method. If it is not implemented, it will simply return
   * {@code null}. If the instrumented library does not expose {@link InetSocketAddress} in its API,
   * you might want to implement this method instead of {@link #getPeerSocketAddress(Object,
   * Object)}.
   */
  @Nullable
  default String getSockFamily(REQUEST request, @Nullable RESPONSE response) {
    return InetSocketAddressUtil.getSockFamily(getServerInetSocketAddress(request, response), null);
  }

  /**
   * Returns the remote socket peer address: IPv4 or IPv6 for internet protocols, path for local
   * communication, etc.
   *
   * <p>Examples: {@code 127.0.0.1}, {@code /tmp/mysql.sock}
   *
   * <p>By default, this method attempts to retrieve the peer address using the {@link
   * #getPeerSocketAddress(Object, Object)} method. If this method is not implemented, it will
   * simply return {@code null}. If the instrumented library does not expose {@link
   * InetSocketAddress} in its API, you might want to implement this method instead of {@link
   * #getPeerSocketAddress(Object, Object)}.
   *
   * @deprecated This method is deprecated and will be removed in the following release. Implement
   *     {@link #getServerSocketAddress(Object, Object)} instead.
   */
  @Deprecated
  @Nullable
  default String getSockPeerAddr(REQUEST request, @Nullable RESPONSE response) {
    return InetSocketAddressUtil.getIpAddress(getServerInetSocketAddress(request, response));
  }

  /**
   * Returns the domain name of an immediate peer.
   *
   * <p>Examples: {@code proxy.example.com}
   *
   * <p>By default, this method attempts to retrieve the peer host name using the {@link
   * #getPeerSocketAddress(Object, Object)} method. If this method is not implemented, it will
   * simply return {@code null}. If the instrumented library does not expose {@link
   * InetSocketAddress} in its API, you might want to implement this method instead of {@link
   * #getPeerSocketAddress(Object, Object)}.
   *
   * @deprecated This method is deprecated and will be removed in the following release. Implement
   *     {@link #getServerSocketDomain(Object, Object)} instead.
   */
  @Deprecated
  @Nullable
  default String getSockPeerName(REQUEST request, @Nullable RESPONSE response) {
    return InetSocketAddressUtil.getDomainName(getServerInetSocketAddress(request, response));
  }

  /**
   * Returns the remote socket peer port.
   *
   * <p>Examples: {@code 16456}
   *
   * <p>By default, this method attempts to retrieve the peer port using the {@link
   * #getPeerSocketAddress(Object, Object)} method. If this method is not implemented, it will
   * simply return {@code null}. If the instrumented library does not expose {@link
   * InetSocketAddress} in its API, you might want to implement this method instead of {@link
   * #getPeerSocketAddress(Object, Object)}.
   *
   * @deprecated This method is deprecated and will be removed in the following release. Implement
   *     {@link #getServerSocketPort(Object, Object)} instead.
   */
  @Deprecated
  @Nullable
  default Integer getSockPeerPort(REQUEST request, @Nullable RESPONSE response) {
    return InetSocketAddressUtil.getPort(getServerInetSocketAddress(request, response));
  }

  /** {@inheritDoc} */
  @Nullable
  @Override
  default String getServerSocketDomain(REQUEST request, @Nullable RESPONSE response) {
    return getSockPeerName(request, response);
  }

  /** {@inheritDoc} */
  @Nullable
  @Override
  default String getServerSocketAddress(REQUEST request, @Nullable RESPONSE response) {
    return getSockPeerAddr(request, response);
  }

  /** {@inheritDoc} */
  @Nullable
  @Override
  default Integer getServerSocketPort(REQUEST request, @Nullable RESPONSE response) {
    return getSockPeerPort(request, response);
  }
}
