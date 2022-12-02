/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.pulsar.telemetry;

import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.instrumentation.api.instrumenter.messaging.MessagingAttributesGetter;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import org.apache.pulsar.client.api.Message;
import javax.annotation.Nullable;

class ConsumerReceiveAttributeGetter
    implements MessagingAttributesGetter<Message<?>, Attributes> {
  public static final ConsumerReceiveAttributeGetter INSTANCE =
      new ConsumerReceiveAttributeGetter();

  private ConsumerReceiveAttributeGetter() {}

  @Nullable
  @Override
  public String system(Message<?> message) {
    return "pulsar";
  }

  @Nullable
  @Override
  public String destinationKind(Message<?> message) {
    return SemanticAttributes.MessagingDestinationKindValues.TOPIC;
  }

  @Nullable
  @Override
  public String destination(Message<?> message) {
    return null;
  }

  @Override
  public boolean temporaryDestination(Message<?> message) {
    return false;
  }

  @Nullable
  @Override
  public String protocol(Message<?> message) {
    return null;
  }

  @Nullable
  @Override
  public String protocolVersion(Message<?> message) {
    return null;
  }

  @Nullable
  @Override
  public String url(Message<?> message) {
    return null;
  }

  @Nullable
  @Override
  public String conversationId(Message<?> message) {
    return null;
  }

  @Nullable
  @Override
  public Long messagePayloadSize(Message<?> message) {
    if (message != null) {
      return (long) message.size();
    }

    return null;
  }

  @Nullable
  @Override
  public Long messagePayloadCompressedSize(Message<?> message) {
    return null;
  }

  @Nullable
  @Override
  public String messageId(Message<?> message, @Nullable Attributes attributes) {
    String messageId0 = null;
    if (null != message && null != message.getMessageId()) {
      messageId0 = message.getMessageId().toString();
    }

    return messageId0;
  }
}
