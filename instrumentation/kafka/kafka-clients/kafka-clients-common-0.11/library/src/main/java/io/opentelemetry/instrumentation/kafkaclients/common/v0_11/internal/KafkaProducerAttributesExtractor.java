/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import java.nio.ByteBuffer;
import javax.annotation.Nullable;
import org.apache.kafka.clients.producer.RecordMetadata;

final class KafkaProducerAttributesExtractor
    implements AttributesExtractor<KafkaProducerRequest, RecordMetadata> {
  // copied from MessagingIncubatingAttributes
  private static final AttributeKey<String> MESSAGING_DESTINATION_PARTITION_ID =
      AttributeKey.stringKey("messaging.destination.partition.id");
  private static final AttributeKey<String> MESSAGING_KAFKA_MESSAGE_KEY =
      AttributeKey.stringKey("messaging.kafka.message.key");
  private static final AttributeKey<Long> MESSAGING_KAFKA_MESSAGE_OFFSET =
      AttributeKey.longKey("messaging.kafka.message.offset");
  private static final AttributeKey<Boolean> MESSAGING_KAFKA_MESSAGE_TOMBSTONE =
      AttributeKey.booleanKey("messaging.kafka.message.tombstone");

  @Override
  public void onStart(
      AttributesBuilder attributes, Context parentContext, KafkaProducerRequest request) {

    Object key = request.getRecord().key();
    if (key != null && canSerialize(key.getClass())) {
      attributes.put(MESSAGING_KAFKA_MESSAGE_KEY, key.toString());
    }
    if (request.getRecord().value() == null) {
      attributes.put(MESSAGING_KAFKA_MESSAGE_TOMBSTONE, true);
    }
  }

  private static boolean canSerialize(Class<?> keyClass) {
    // we make a simple assumption here that we can serialize keys by simply calling toString()
    // and that does not work for byte[] or ByteBuffer
    return !(keyClass.isArray() || keyClass == ByteBuffer.class);
  }

  @Override
  public void onEnd(
      AttributesBuilder attributes,
      Context context,
      KafkaProducerRequest request,
      @Nullable RecordMetadata recordMetadata,
      @Nullable Throwable error) {

    if (recordMetadata != null) {
      attributes.put(
          MESSAGING_DESTINATION_PARTITION_ID, String.valueOf(recordMetadata.partition()));
      attributes.put(MESSAGING_KAFKA_MESSAGE_OFFSET, recordMetadata.offset());
    }
  }
}
