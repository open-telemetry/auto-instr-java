/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.incubator.semconv.messaging;

import static io.opentelemetry.semconv.NetworkAttributes.NETWORK_PEER_ADDRESS;
import static io.opentelemetry.semconv.NetworkAttributes.NETWORK_PEER_PORT;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_ADDRESS;
import static io.opentelemetry.semconv.ServerAttributes.SERVER_PORT;

import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import javax.annotation.Nullable;

public final class MessagingNetworkAttributesExtractor<REQUEST, RESPONSE>
    implements AttributesExtractor<REQUEST, RESPONSE> {

  /**
   * Creates the network attributes extractor.
   *
   * @see InstrumenterBuilder#addAttributesExtractor(AttributesExtractor)
   */
  public static <REQUEST, RESPONSE> MessagingNetworkAttributesExtractor<REQUEST, RESPONSE> create(
      MessagingNetworkAttributesGetter<REQUEST, RESPONSE> getter) {
    return new MessagingNetworkAttributesExtractor<>(getter);
  }

  private final MessagingNetworkAttributesGetter<REQUEST, RESPONSE> getter;

  MessagingNetworkAttributesExtractor(MessagingNetworkAttributesGetter<REQUEST, RESPONSE> getter) {
    this.getter = getter;
  }

  @Override
  public void onStart(AttributesBuilder attributes, Context parentContext, REQUEST request) {
    attributes.put(NETWORK_PEER_ADDRESS, getter.getNetworkPeerAddress(request, null));
    Integer networkPeerPort = getter.getNetworkPeerPort(request, null);
    if (networkPeerPort != null) {
      attributes.put(NETWORK_PEER_PORT, networkPeerPort);
    }

    attributes.put(SERVER_ADDRESS, getter.getServerAddress(request));
    Integer serverPort = getter.getServerPort(request);
    if (serverPort != null) {
      attributes.put(SERVER_PORT, serverPort);
    }
  }

  @Override
  public void onEnd(
      AttributesBuilder attributes,
      Context context,
      REQUEST request,
      @Nullable RESPONSE response,
      @Nullable Throwable error) {}
}
