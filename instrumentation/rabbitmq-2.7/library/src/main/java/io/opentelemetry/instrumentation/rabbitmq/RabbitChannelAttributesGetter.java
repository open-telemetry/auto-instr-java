/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.rabbitmq;

import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesGetter;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;

enum RabbitChannelAttributesGetter implements MessagingAttributesGetter<InstrumentedChannel, Void> {
  INSTANCE;

  @Override
  public String getSystem(InstrumentedChannel channelAndMethod) {
    return "rabbitmq";
  }

  @Nullable
  @Override
  public String getDestination(InstrumentedChannel channelAndMethod) {
    return null;
  }

  @Override
  public boolean isTemporaryDestination(InstrumentedChannel channelAndMethod) {
    return false;
  }

  @Nullable
  @Override
  public String getConversationId(InstrumentedChannel channelAndMethod) {
    return null;
  }

  @Nullable
  @Override
  public Long getMessagePayloadSize(InstrumentedChannel channelAndMethod) {
    return null;
  }

  @Nullable
  @Override
  public Long getMessagePayloadCompressedSize(InstrumentedChannel channelAndMethod) {
    return null;
  }

  @Nullable
  @Override
  public String getMessageId(InstrumentedChannel channelAndMethod, @Nullable Void unused) {
    return null;
  }

  @Override
  public List<String> getMessageHeader(InstrumentedChannel instrumentedChannel, String name) {
    if (instrumentedChannel.getHeaders() != null) {
      Object value = instrumentedChannel.getHeaders().get(name);
      if (value != null) {
        return Collections.singletonList(value.toString());
      }
    }
    return Collections.emptyList();
  }
}
