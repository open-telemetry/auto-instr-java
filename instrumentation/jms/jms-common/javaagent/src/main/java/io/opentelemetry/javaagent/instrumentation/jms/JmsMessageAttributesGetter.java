/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.jms;

import static java.util.logging.Level.FINE;

import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesGetter;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingNetworkAttributesGetter;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import javax.annotation.Nullable;

enum JmsMessageAttributesGetter
    implements
        MessagingAttributesGetter<MessageWithDestination, Void>,
        MessagingNetworkAttributesGetter<MessageWithDestination, Void> {
  INSTANCE;

  private static final Logger logger = Logger.getLogger(JmsMessageAttributesGetter.class.getName());

  @Override
  public String getSystem(MessageWithDestination messageWithDestination) {
    return "jms";
  }

  @Nullable
  @Override
  public String getDestination(MessageWithDestination messageWithDestination) {
    return messageWithDestination.destinationName();
  }

  @Nullable
  @Override
  public String getDestinationTemplate(MessageWithDestination messageWithDestination) {
    return null;
  }

  @Override
  public boolean isTemporaryDestination(MessageWithDestination messageWithDestination) {
    return messageWithDestination.isTemporaryDestination();
  }

  @Override
  public boolean isAnonymousDestination(MessageWithDestination messageWithDestination) {
    return false;
  }

  @Nullable
  @Override
  public String getConversationId(MessageWithDestination messageWithDestination) {
    try {
      return messageWithDestination.message().getJmsCorrelationId();
    } catch (Exception e) {
      logger.log(FINE, "Failure getting JMS correlation id", e);
      return null;
    }
  }

  @Nullable
  @Override
  public Long getMessageBodySize(MessageWithDestination messageWithDestination) {
    return null;
  }

  @Nullable
  @Override
  public Long getMessageEnvelopeSize(MessageWithDestination messageWithDestination) {
    return null;
  }

  @Nullable
  @Override
  public String getMessageId(MessageWithDestination messageWithDestination, Void unused) {
    try {
      return messageWithDestination.message().getJmsMessageId();
    } catch (Exception exception) {
      logger.log(FINE, "Failure getting JMS message id", exception);
      return null;
    }
  }

  @Nullable
  @Override
  public String getClientId(MessageWithDestination messageWithDestination) {
    return null;
  }

  @Nullable
  @Override
  public Long getBatchMessageCount(
      MessageWithDestination messageWithDestination, @Nullable Void unused) {
    return null;
  }

  @Override
  public List<String> getMessageHeader(MessageWithDestination messageWithDestination, String name) {
    try {
      String value = messageWithDestination.message().getStringProperty(name);
      if (value != null) {
        return Collections.singletonList(value);
      }
    } catch (Exception exception) {
      logger.log(FINE, "Failure getting JMS message header", exception);
    }
    return Collections.emptyList();
  }
}
