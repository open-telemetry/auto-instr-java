/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.kafka.internal;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public class KafkaProcessRequest extends AbstractKafkaConsumerRequest {

  private final ConsumerRecord<?, ?> record;

  public static KafkaProcessRequest create(ConsumerRecord<?, ?> record, Consumer<?, ?> consumer) {
    return create(record, KafkaUtil.getConsumerGroup(consumer), KafkaUtil.getClientId(consumer));
  }

  public static KafkaProcessRequest create(
      KafkaConsumerContext consumerContext, ConsumerRecord<?, ?> record) {
    return create(record, consumerContext != null ? consumerContext.getConsumer() : null);
  }

  public static KafkaProcessRequest create(
      ConsumerRecord<?, ?> record, String consumerGroup, String clientId) {
    return new KafkaProcessRequest(record, consumerGroup, clientId);
  }

  public KafkaProcessRequest(ConsumerRecord<?, ?> record, String consumerGroup, String clientId) {
    super(consumerGroup, clientId);
    this.record = record;
  }

  public ConsumerRecord<?, ?> getRecord() {
    return record;
  }
}
