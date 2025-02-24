/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.rocketmqclient.v4_8;

import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesGetter;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingNetworkAttributesGetter;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.rocketmq.client.hook.SendMessageContext;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;

enum RocketMqProducerAttributeGetter
    implements
        MessagingAttributesGetter<SendMessageContext, Void>,
        MessagingNetworkAttributesGetter<SendMessageContext, Void> {
  INSTANCE;

  @Override
  public String getSystem(SendMessageContext request) {
    return "rocketmq";
  }

  @Nullable
  @Override
  public String getDestination(SendMessageContext request) {
    Message message = request.getMessage();
    return message == null ? null : message.getTopic();
  }

  @Nullable
  @Override
  public String getDestinationTemplate(SendMessageContext request) {
    return null;
  }

  @Override
  public boolean isTemporaryDestination(SendMessageContext request) {
    return false;
  }

  @Override
  public boolean isAnonymousDestination(SendMessageContext request) {
    return false;
  }

  @Nullable
  @Override
  public String getConversationId(SendMessageContext request) {
    return null;
  }

  @Nullable
  @Override
  public Long getMessageBodySize(SendMessageContext request) {
    return null;
  }

  @Nullable
  @Override
  public Long getMessageEnvelopeSize(SendMessageContext request) {
    return null;
  }

  @Nullable
  @Override
  public String getMessageId(SendMessageContext request, @Nullable Void unused) {
    SendResult sendResult = request.getSendResult();
    return sendResult == null ? null : sendResult.getMsgId();
  }

  @Nullable
  @Override
  public String getClientId(SendMessageContext request) {
    return null;
  }

  @Nullable
  @Override
  public Long getBatchMessageCount(SendMessageContext request, @Nullable Void unused) {
    return null;
  }

  @Override
  public List<String> getMessageHeader(SendMessageContext request, String name) {
    String value = request.getMessage().getProperties().get(name);
    if (value != null) {
      return Collections.singletonList(value);
    }
    return Collections.emptyList();
  }
}
