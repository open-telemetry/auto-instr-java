/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal;

import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesGetter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.annotation.Nullable;
import org.apache.kafka.clients.producer.RecordMetadata;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
enum KafkaProducerAttributesGetter
    implements MessagingAttributesGetter<KafkaProducerRequest, RecordMetadata> {
  INSTANCE;

  @Override
  public String getSystem(KafkaProducerRequest request) {
    return "kafka";
  }

  @Override
  public String getDestination(KafkaProducerRequest request) {
    return request.getRecord().topic();
  }

  @Nullable
  @Override
  public String getDestinationTemplate(KafkaProducerRequest request) {
    return null;
  }

  @Override
  public boolean isTemporaryDestination(KafkaProducerRequest request) {
    return false;
  }

  @Override
  public boolean isAnonymousDestination(KafkaProducerRequest request) {
    return false;
  }

  @Override
  @Nullable
  public String getConversationId(KafkaProducerRequest request) {
    return null;
  }

  @Nullable
  @Override
  public Long getMessageBodySize(KafkaProducerRequest request) {
    return null;
  }

  @Nullable
  @Override
  public Long getMessageEnvelopeSize(KafkaProducerRequest request) {
    return null;
  }

  @Override
  @Nullable
  public String getMessageId(
      KafkaProducerRequest request, @Nullable RecordMetadata recordMetadata) {
    return null;
  }

  @Nullable
  @Override
  public String getClientId(KafkaProducerRequest request) {
    return request.getClientId();
  }

  @Nullable
  @Override
  public Long getBatchMessageCount(
      KafkaProducerRequest request, @Nullable RecordMetadata recordMetadata) {
    return null;
  }

  @Override
  public List<String> getMessageHeader(KafkaProducerRequest request, String name) {
    return StreamSupport.stream(request.getRecord().headers().headers(name).spliterator(), false)
        .map(header -> new String(header.value(), StandardCharsets.UTF_8))
        .collect(Collectors.toList());
  }
}
