/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.pulsar.v2_8.telemetry;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesGetter;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingNetworkAttributesGetter;
import java.net.InetSocketAddress;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.common.naming.TopicName;

enum PulsarMessagingAttributesGetter
    implements
        MessagingAttributesGetter<PulsarRequest, Void>,
        MessagingNetworkAttributesGetter<PulsarRequest, Void> {
  INSTANCE;

  @Override
  public String getSystem(PulsarRequest request) {
    return "pulsar";
  }

  @Nullable
  @Override
  public String getDestination(PulsarRequest request) {
    return request.getDestination();
  }

  @Nullable
  @Override
  public String getDestinationTemplate(PulsarRequest request) {
    return null;
  }

  @Override
  public boolean isTemporaryDestination(PulsarRequest request) {
    return false;
  }

  @Override
  public boolean isAnonymousDestination(PulsarRequest request) {
    return false;
  }

  @Nullable
  @Override
  public String getConversationId(PulsarRequest message) {
    return null;
  }

  @Override
  public Long getMessageBodySize(PulsarRequest request) {
    return (long) request.getMessage().size();
  }

  @Nullable
  @Override
  public Long getMessageEnvelopeSize(PulsarRequest request) {
    return null;
  }

  @Nullable
  @Override
  public String getMessageId(PulsarRequest request, @Nullable Void response) {
    Message<?> message = request.getMessage();
    if (message.getMessageId() != null) {
      return message.getMessageId().toString();
    }

    return null;
  }

  @Nullable
  @Override
  public String getClientId(PulsarRequest request) {
    return null;
  }

  @Nullable
  @Override
  public Long getBatchMessageCount(PulsarRequest request, @Nullable Void unused) {
    return null;
  }

  @Nullable
  @Override
  public String getDestinationPartitionId(PulsarRequest request) {
    int partitionIndex = TopicName.getPartitionIndex(request.getDestination());
    if (partitionIndex == -1) {
      return null;
    }
    return String.valueOf(partitionIndex);
  }

  @Override
  public List<String> getMessageHeader(PulsarRequest request, String name) {
    String value = request.getMessage().getProperty(name);
    return value != null ? singletonList(value) : emptyList();
  }

  @Nullable
  @Override
  public Integer getServerPort(PulsarRequest pulsarRequest) {
    return PulsarNetClientAttributesGetter.INSTANCE.getServerPort(pulsarRequest);
  }

  @Nullable
  @Override
  public String getServerAddress(PulsarRequest pulsarRequest) {
    return PulsarNetClientAttributesGetter.INSTANCE.getServerAddress(pulsarRequest);
  }

  @Nullable
  @Override
  public InetSocketAddress getNetworkPeerInetSocketAddress(
      PulsarRequest pulsarRequest, @Nullable Void unused) {
    return PulsarNetClientAttributesGetter.INSTANCE.getNetworkPeerInetSocketAddress(
        pulsarRequest, null);
  }

  @Nullable
  @Override
  public Integer getNetworkPeerPort(PulsarRequest pulsarRequest, @Nullable Void unused) {
    return PulsarNetClientAttributesGetter.INSTANCE.getNetworkPeerPort(pulsarRequest, null);
  }
}
