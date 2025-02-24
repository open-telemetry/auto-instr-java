/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.rocketmqclient.v5_0;

import apache.rocketmq.v2.ReceiveMessageRequest;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesGetter;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingNetworkAttributesGetter;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.rocketmq.client.apis.message.MessageView;

enum RocketMqConsumerReceiveAttributeGetter
    implements
        MessagingAttributesGetter<ReceiveMessageRequest, List<MessageView>>,
        MessagingNetworkAttributesGetter<ReceiveMessageRequest, List<MessageView>> {
  INSTANCE;

  @Nullable
  @Override
  public String getSystem(ReceiveMessageRequest request) {
    return "rocketmq";
  }

  @Nullable
  @Override
  public String getDestination(ReceiveMessageRequest request) {
    return request.getMessageQueue().getTopic().getName();
  }

  @Nullable
  @Override
  public String getDestinationTemplate(ReceiveMessageRequest request) {
    return null;
  }

  @Override
  public boolean isTemporaryDestination(ReceiveMessageRequest request) {
    return false;
  }

  @Override
  public boolean isAnonymousDestination(ReceiveMessageRequest request) {
    return false;
  }

  @Nullable
  @Override
  public String getConversationId(ReceiveMessageRequest request) {
    return null;
  }

  @Nullable
  @Override
  public Long getMessageBodySize(ReceiveMessageRequest request) {
    return null;
  }

  @Nullable
  @Override
  public Long getMessageEnvelopeSize(ReceiveMessageRequest request) {
    return null;
  }

  @Nullable
  @Override
  public String getMessageId(ReceiveMessageRequest request, @Nullable List<MessageView> unused) {
    return null;
  }

  @Nullable
  @Override
  public String getClientId(ReceiveMessageRequest request) {
    return null;
  }

  @Nullable
  @Override
  public Long getBatchMessageCount(
      ReceiveMessageRequest request, @Nullable List<MessageView> messages) {
    return messages != null ? (long) messages.size() : null;
  }
}
