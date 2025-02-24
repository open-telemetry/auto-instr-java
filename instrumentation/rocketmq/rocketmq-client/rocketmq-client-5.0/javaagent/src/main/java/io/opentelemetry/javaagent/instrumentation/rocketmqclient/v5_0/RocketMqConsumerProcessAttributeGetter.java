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
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.message.MessageView;

enum RocketMqConsumerProcessAttributeGetter
    implements
        MessagingAttributesGetter<MessageView, ConsumeResult>,
        MessagingNetworkAttributesGetter<MessageView, ConsumeResult> {
  INSTANCE;

  @Nullable
  @Override
  public String getSystem(MessageView messageView) {
    return "rocketmq";
  }

  @Nullable
  @Override
  public String getDestination(MessageView messageView) {
    return messageView.getTopic();
  }

  @Nullable
  @Override
  public String getDestinationTemplate(MessageView messageView) {
    return null;
  }

  @Override
  public boolean isTemporaryDestination(MessageView messageView) {
    return false;
  }

  @Override
  public boolean isAnonymousDestination(MessageView messageView) {
    return false;
  }

  @Nullable
  @Override
  public String getConversationId(MessageView messageView) {
    return null;
  }

  @Nullable
  @Override
  public Long getMessageBodySize(MessageView messageView) {
    return (long) messageView.getBody().remaining();
  }

  @Nullable
  @Override
  public Long getMessageEnvelopeSize(MessageView messageView) {
    return null;
  }

  @Nullable
  @Override
  public String getMessageId(MessageView messageView, @Nullable ConsumeResult unused) {
    return messageView.getMessageId().toString();
  }

  @Nullable
  @Override
  public String getClientId(MessageView messageView) {
    return null;
  }

  @Nullable
  @Override
  public Long getBatchMessageCount(MessageView messageView, @Nullable ConsumeResult unused) {
    return null;
  }

  @Override
  public List<String> getMessageHeader(MessageView messageView, String name) {
    String value = messageView.getProperties().get(name);
    if (value != null) {
      return Collections.singletonList(value);
    }
    return Collections.emptyList();
  }
}
