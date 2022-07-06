/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.instrumenter.net;

import static io.opentelemetry.instrumentation.api.internal.AttributesExtractorUtil.internalSet;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import javax.annotation.Nullable;

/**
 * Extractor of <a
 * href="https://github.com/open-telemetry/opentelemetry-specification/blob/main/specification/trace/semantic_conventions/span-general.md#general-network-connection-attributes">Network
 * attributes</a>. It is common to have access to {@link java.net.InetSocketAddress}, in which case
 * it is more convenient to use {@link InetSocketAddressNetClientAttributesGetter}.
 *
 * <p>This class delegates to a type-specific {@link NetClientAttributesGetter} for individual
 * attribute extraction from request/response objects.
 */
public final class NetClientAttributesExtractor<REQUEST, RESPONSE>
    implements AttributesExtractor<REQUEST, RESPONSE> {

  private final NetClientAttributesGetter<REQUEST, RESPONSE> getter;

  public static <REQUEST, RESPONSE> NetClientAttributesExtractor<REQUEST, RESPONSE> create(
      NetClientAttributesGetter<REQUEST, RESPONSE> getter) {
    return new NetClientAttributesExtractor<>(getter);
  }

  private NetClientAttributesExtractor(NetClientAttributesGetter<REQUEST, RESPONSE> getter) {
    this.getter = getter;
  }

  @Override
  public void onStart(AttributesBuilder attributes, Context parentContext, REQUEST request) {}

  @Override
  public void onEnd(
      AttributesBuilder attributes,
      Context context,
      REQUEST request,
      @Nullable RESPONSE response,
      @Nullable Throwable error) {

    internalSet(attributes, SemanticAttributes.NET_TRANSPORT, getter.transport(request, response));

    String peerName = getter.peerName(request, response);
    String sockPeerAddr = getter.sockPeerAddr(request, response);

    Integer peerPort = getter.peerPort(request, response);
    Integer sockPeerPort = getter.sockPeerPort(request, response);

    if (peerName != null && !peerName.equals(sockPeerAddr)) {
      internalSet(attributes, SemanticAttributes.NET_PEER_NAME, peerName);
      if (peerPort != null && peerPort > 0) {
        internalSet(attributes, SemanticAttributes.NET_PEER_PORT, (long) peerPort);
      }
      internalSet(attributes, AttributeKey.stringKey("net.sock.peer.addr"), sockPeerAddr);
      if (sockPeerPort != null && sockPeerPort > 0 && !sockPeerPort.equals(peerPort)) {
        internalSet(attributes, AttributeKey.longKey("net.sock.peer.port"), (long) sockPeerPort);
      }
    } else {
      internalSet(attributes, AttributeKey.stringKey("net.sock.peer.addr"), sockPeerAddr);
      if (sockPeerPort != null && sockPeerPort > 0) {
        internalSet(attributes, AttributeKey.longKey("net.sock.peer.port"), (long) sockPeerPort);
      }
    }
  }
}
