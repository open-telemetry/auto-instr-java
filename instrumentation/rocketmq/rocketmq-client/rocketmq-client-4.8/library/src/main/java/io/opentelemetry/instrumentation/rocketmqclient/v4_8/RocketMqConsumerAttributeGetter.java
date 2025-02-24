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
import org.apache.rocketmq.common.message.MessageExt;

enum RocketMqConsumerAttributeGetter
    implements
        MessagingAttributesGetter<MessageExt, Void>,
        MessagingNetworkAttributesGetter<MessageExt, Void> {
  INSTANCE;

  @Override
  public String getSystem(MessageExt request) {
    return "rocketmq";
  }

  @Override
  public String getDestination(MessageExt request) {
    return request.getTopic();
  }

  @Nullable
  @Override
  public String getDestinationTemplate(MessageExt request) {
    return null;
  }

  @Override
  public boolean isTemporaryDestination(MessageExt request) {
    return false;
  }

  @Override
  public boolean isAnonymousDestination(MessageExt request) {
    return false;
  }

  @Nullable
  @Override
  public String getConversationId(MessageExt request) {
    return null;
  }

  @Nullable
  @Override
  public Long getMessageBodySize(MessageExt request) {
    byte[] body = request.getBody();
    return body == null ? null : (long) body.length;
  }

  @Nullable
  @Override
  public Long getMessageEnvelopeSize(MessageExt request) {
    return null;
  }

  @Nullable
  @Override
  public String getMessageId(MessageExt request, @Nullable Void unused) {
    return request.getMsgId();
  }

  @Nullable
  @Override
  public String getClientId(MessageExt request) {
    return null;
  }

  @Nullable
  @Override
  public Long getBatchMessageCount(MessageExt request, @Nullable Void unused) {
    return null;
  }

  @Override
  public List<String> getMessageHeader(MessageExt request, String name) {
    String value = request.getProperties().get(name);
    if (value != null) {
      return Collections.singletonList(value);
    }
    return Collections.emptyList();
  }
}
