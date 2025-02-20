/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.rocketmqclient.v5_0;

import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesGetter;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingNetworkAttributesGetter;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.rocketmq.client.java.impl.producer.SendReceiptImpl;
import org.apache.rocketmq.client.java.message.PublishingMessageImpl;

enum RocketMqProducerAttributeGetter
    implements
        MessagingAttributesGetter<PublishingMessageImpl, SendReceiptImpl>,
        MessagingNetworkAttributesGetter<PublishingMessageImpl, SendReceiptImpl> {
  INSTANCE;

  @Nullable
  @Override
  public String getSystem(PublishingMessageImpl message) {
    return "rocketmq";
  }

  @Nullable
  @Override
  public String getDestination(PublishingMessageImpl message) {
    return message.getTopic();
  }

  @Nullable
  @Override
  public String getDestinationTemplate(PublishingMessageImpl message) {
    return null;
  }

  @Override
  public boolean isTemporaryDestination(PublishingMessageImpl message) {
    return false;
  }

  @Override
  public boolean isAnonymousDestination(PublishingMessageImpl message) {
    return false;
  }

  @Nullable
  @Override
  public String getConversationId(PublishingMessageImpl message) {
    return null;
  }

  @Override
  public Long getMessageBodySize(PublishingMessageImpl message) {
    return (long) message.getBody().remaining();
  }

  @Nullable
  @Override
  public Long getMessageEnvelopeSize(PublishingMessageImpl message) {
    return null;
  }

  @Nullable
  @Override
  public String getMessageId(PublishingMessageImpl message, @Nullable SendReceiptImpl sendReceipt) {
    return message.getMessageId().toString();
  }

  @Nullable
  @Override
  public String getClientId(PublishingMessageImpl message) {
    return null;
  }

  @Nullable
  @Override
  public Long getBatchMessageCount(
      PublishingMessageImpl publishingMessage, @Nullable SendReceiptImpl sendReceipt) {
    return null;
  }

  @Override
  public List<String> getMessageHeader(PublishingMessageImpl message, String name) {
    String value = message.getProperties().get(name);
    if (value != null) {
      return Collections.singletonList(value);
    }
    return Collections.emptyList();
  }
}
