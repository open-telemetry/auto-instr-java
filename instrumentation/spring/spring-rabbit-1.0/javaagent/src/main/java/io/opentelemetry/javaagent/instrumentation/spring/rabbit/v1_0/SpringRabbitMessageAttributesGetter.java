/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.spring.rabbit.v1_0;

import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesGetter;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingNetworkAttributesGetter;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import org.springframework.amqp.core.Message;

enum SpringRabbitMessageAttributesGetter
    implements
        MessagingAttributesGetter<Message, Void>, MessagingNetworkAttributesGetter<Message, Void> {
  INSTANCE;

  @Override
  public String getSystem(Message message) {
    return "rabbitmq";
  }

  @Override
  @Nullable
  public String getDestination(Message message) {
    return message.getMessageProperties().getReceivedRoutingKey();
  }

  @Nullable
  @Override
  public String getDestinationTemplate(Message message) {
    return null;
  }

  @Override
  public boolean isTemporaryDestination(Message message) {
    return false;
  }

  @Override
  public boolean isAnonymousDestination(Message message) {
    return false;
  }

  @Override
  @Nullable
  public String getConversationId(Message message) {
    return null;
  }

  @Override
  public Long getMessageBodySize(Message message) {
    return message.getMessageProperties().getContentLength();
  }

  @Nullable
  @Override
  public Long getMessageEnvelopeSize(Message message) {
    return null;
  }

  @Override
  @Nullable
  public String getMessageId(Message message, @Nullable Void unused) {
    return message.getMessageProperties().getMessageId();
  }

  @Nullable
  @Override
  public String getClientId(Message message) {
    return null;
  }

  @Nullable
  @Override
  public Long getBatchMessageCount(Message message, @Nullable Void unused) {
    return null;
  }

  @Override
  public List<String> getMessageHeader(Message message, String name) {
    Object value = message.getMessageProperties().getHeaders().get(name);
    if (value != null) {
      return Collections.singletonList(value.toString());
    }
    return Collections.emptyList();
  }
}
