/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.spring.pulsar.v1_0;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesGetter;
import java.util.List;
import javax.annotation.Nullable;
import org.apache.pulsar.client.api.Message;

enum SpringPulsarMessageAttributesGetter implements MessagingAttributesGetter<Message<?>, Void> {
  INSTANCE;

  @Override
  public String getSystem(Message<?> message) {
    return "pulsar";
  }

  @Override
  @Nullable
  public String getDestination(Message<?> message) {
    return message.getTopicName();
  }

  @Nullable
  @Override
  public String getDestinationTemplate(Message<?> message) {
    return null;
  }

  @Override
  public boolean isTemporaryDestination(Message<?> message) {
    return false;
  }

  @Override
  public boolean isAnonymousDestination(Message<?> message) {
    return false;
  }

  @Override
  @Nullable
  public String getConversationId(Message<?> message) {
    return null;
  }

  @Override
  public Long getMessageBodySize(Message<?> message) {
    return (long) message.size();
  }

  @Nullable
  @Override
  public Long getMessageEnvelopeSize(Message<?> message) {
    return null;
  }

  @Override
  @Nullable
  public String getMessageId(Message<?> message, @Nullable Void unused) {
    if (message.getMessageId() != null) {
      return message.getMessageId().toString();
    }

    return null;
  }

  @Nullable
  @Override
  public String getClientId(Message<?> message) {
    return null;
  }

  @Nullable
  @Override
  public Long getBatchMessageCount(Message<?> message, @Nullable Void unused) {
    return null;
  }

  @Override
  public List<String> getMessageHeader(Message<?> message, String name) {
    String value = message.getProperty(name);
    return value != null ? singletonList(value) : emptyList();
  }
}
