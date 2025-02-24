/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal;

import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.util.VirtualField;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class KafkaConsumerContextUtil {
  // these fields can be used for multiple instrumentations because of that we don't use a helper
  // class as field type
  private static final VirtualField<ConsumerRecord<?, ?>, Context> recordContextField =
      VirtualField.find(ConsumerRecord.class, Context.class);
  private static final VirtualField<ConsumerRecord<?, ?>, String[]> recordConsumerInfoField =
      VirtualField.find(ConsumerRecord.class, String[].class);
  private static final VirtualField<ConsumerRecords<?, ?>, Context> recordsContextField =
      VirtualField.find(ConsumerRecords.class, Context.class);
  private static final VirtualField<ConsumerRecords<?, ?>, String[]> recordsConsumerInfoField =
      VirtualField.find(ConsumerRecords.class, String[].class);

  public static KafkaConsumerContext get(ConsumerRecord<?, ?> records) {
    Context receiveContext = recordContextField.get(records);
    String consumerGroup = null;
    String clientId = null;
    String[] consumerInfo = recordConsumerInfoField.get(records);
    if (consumerInfo != null) {
      consumerGroup = consumerInfo[0];
      clientId = consumerInfo[1];
    }
    return create(receiveContext, consumerGroup, clientId);
  }

  public static KafkaConsumerContext get(ConsumerRecords<?, ?> records) {
    Context receiveContext = recordsContextField.get(records);
    String consumerGroup = null;
    String clientId = null;
    String[] consumerInfo = recordsConsumerInfoField.get(records);
    if (consumerInfo != null) {
      consumerGroup = consumerInfo[0];
      clientId = consumerInfo[1];
    }
    return create(receiveContext, consumerGroup, clientId);
  }

  public static KafkaConsumerContext create(Context context, Consumer<?, ?> consumer) {
    return create(context, KafkaUtil.getConsumerGroup(consumer), KafkaUtil.getClientId(consumer));
  }

  public static KafkaConsumerContext create(
      Context context, String consumerGroup, String clientId) {
    return KafkaConsumerContext.create(context, consumerGroup, clientId);
  }

  public static void set(ConsumerRecord<?, ?> record, Context context, Consumer<?, ?> consumer) {
    recordContextField.set(record, context);
    String consumerGroup = KafkaUtil.getConsumerGroup(consumer);
    String clientId = KafkaUtil.getClientId(consumer);
    set(record, context, consumerGroup, clientId);
  }

  public static void set(ConsumerRecord<?, ?> record, KafkaConsumerContext consumerContext) {
    set(
        record,
        consumerContext.getContext(),
        consumerContext.getConsumerGroup(),
        consumerContext.getClientId());
  }

  public static void set(
      ConsumerRecord<?, ?> record, Context context, String consumerGroup, String clientId) {
    recordContextField.set(record, context);
    recordConsumerInfoField.set(record, new String[] {consumerGroup, clientId});
  }

  public static void set(ConsumerRecords<?, ?> records, Context context, Consumer<?, ?> consumer) {
    String consumerGroup = KafkaUtil.getConsumerGroup(consumer);
    String clientId = KafkaUtil.getClientId(consumer);
    set(records, context, consumerGroup, clientId);
  }

  public static void set(
      ConsumerRecords<?, ?> records, Context context, String consumerGroup, String clientId) {
    recordsContextField.set(records, context);
    recordsConsumerInfoField.set(records, new String[] {consumerGroup, clientId});
  }

  public static void copy(ConsumerRecord<?, ?> from, ConsumerRecord<?, ?> to) {
    recordContextField.set(to, recordContextField.get(from));
    recordConsumerInfoField.set(to, recordConsumerInfoField.get(from));
  }

  private KafkaConsumerContextUtil() {}
}
