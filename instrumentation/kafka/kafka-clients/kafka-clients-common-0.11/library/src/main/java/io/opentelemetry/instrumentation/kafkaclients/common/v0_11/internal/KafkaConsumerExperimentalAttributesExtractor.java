/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal;

import static io.opentelemetry.api.common.AttributeKey.longKey;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import javax.annotation.Nullable;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.record.TimestampType;

final class KafkaConsumerExperimentalAttributesExtractor
    implements AttributesExtractor<KafkaProcessRequest, Void> {

  private static final AttributeKey<Long> KAFKA_RECORD_QUEUE_TIME_MS =
      longKey("kafka.record.queue_time_ms");

  @Override
  public void onStart(
      AttributesBuilder attributes, Context parentContext, KafkaProcessRequest request) {

    ConsumerRecord<?, ?> record = request.getRecord();
    // don't record a duration if the message was sent from an old Kafka client
    if (record.timestampType() != TimestampType.NO_TIMESTAMP_TYPE) {
      long produceTime = record.timestamp();
      // this attribute shows how much time elapsed between the producer and the consumer of this
      // message, which can be helpful for identifying queue bottlenecks
      attributes.put(
          KAFKA_RECORD_QUEUE_TIME_MS, Math.max(0L, System.currentTimeMillis() - produceTime));
    }
  }

  @Override
  public void onEnd(
      AttributesBuilder attributes,
      Context context,
      KafkaProcessRequest request,
      @Nullable Void unused,
      @Nullable Throwable error) {}
}
