/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.pulsar.v2_8.telemetry;

import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesGetter;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingNetworkAttributesGetter;
import java.net.InetSocketAddress;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import org.apache.pulsar.common.naming.TopicName;

enum PulsarBatchMessagingAttributesGetter
    implements
        MessagingAttributesGetter<PulsarBatchRequest, Void>,
        MessagingNetworkAttributesGetter<PulsarBatchRequest, Void> {
  INSTANCE;

  @Override
  public String getSystem(PulsarBatchRequest request) {
    return "pulsar";
  }

  @Nullable
  @Override
  public String getDestination(PulsarBatchRequest request) {
    return request.getDestination();
  }

  @Nullable
  @Override
  public String getDestinationTemplate(PulsarBatchRequest request) {
    return null;
  }

  @Override
  public boolean isTemporaryDestination(PulsarBatchRequest request) {
    return false;
  }

  @Override
  public boolean isAnonymousDestination(PulsarBatchRequest request) {
    return false;
  }

  @Nullable
  @Override
  public String getConversationId(PulsarBatchRequest message) {
    return null;
  }

  @Nullable
  @Override
  public Long getMessageBodySize(PulsarBatchRequest request) {
    return StreamSupport.stream(request.getMessages().spliterator(), false)
        .map(message -> (long) message.size())
        .reduce(Long::sum)
        .orElse(null);
  }

  @Nullable
  @Override
  public Long getMessageEnvelopeSize(PulsarBatchRequest request) {
    return null;
  }

  @Nullable
  @Override
  public String getMessageId(PulsarBatchRequest request, @Nullable Void response) {
    return null;
  }

  @Nullable
  @Override
  public String getClientId(PulsarBatchRequest request) {
    return null;
  }

  @Override
  public Long getBatchMessageCount(PulsarBatchRequest request, @Nullable Void unused) {
    return (long) request.getMessages().size();
  }

  @Nullable
  @Override
  public String getDestinationPartitionId(PulsarBatchRequest request) {
    int partitionIndex = TopicName.getPartitionIndex(request.getDestination());
    if (partitionIndex == -1) {
      return null;
    }
    return String.valueOf(partitionIndex);
  }

  @Override
  public List<String> getMessageHeader(PulsarBatchRequest request, String name) {
    return StreamSupport.stream(request.getMessages().spliterator(), false)
        .map(message -> message.getProperty(name))
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  @Nullable
  @Override
  public Integer getServerPort(PulsarBatchRequest pulsarRequest) {
    return PulsarNetClientAttributesGetter.INSTANCE.getServerPort(pulsarRequest);
  }

  @Nullable
  @Override
  public String getServerAddress(PulsarBatchRequest pulsarRequest) {
    return PulsarNetClientAttributesGetter.INSTANCE.getServerAddress(pulsarRequest);
  }

  @Nullable
  @Override
  public InetSocketAddress getNetworkPeerInetSocketAddress(
      PulsarBatchRequest pulsarRequest, @Nullable Void unused) {
    return PulsarNetClientAttributesGetter.INSTANCE.getNetworkPeerInetSocketAddress(
        pulsarRequest, null);
  }

  @Nullable
  @Override
  public Integer getNetworkPeerPort(PulsarBatchRequest pulsarRequest, @Nullable Void unused) {
    return PulsarNetClientAttributesGetter.INSTANCE.getNetworkPeerPort(pulsarRequest, null);
  }
}
