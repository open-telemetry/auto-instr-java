/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal;

import io.opentelemetry.instrumentation.api.util.VirtualField;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class KafkaUtil {

  private static final String CONSUMER_GROUP = "consumer_group";
  private static final String CLIENT_ID = "client_id";

  private static final VirtualField<Consumer<?, ?>, Map<String, String>> consumerInfoField =
      VirtualField.find(Consumer.class, Map.class);

  private static final MethodHandle GET_GROUP_METADATA;
  private static final MethodHandle GET_GROUP_ID;

  static {
    MethodHandle getGroupMetadata;
    MethodHandle getGroupId;

    try {
      Class<?> consumerGroupMetadata =
          Class.forName("org.apache.kafka.clients.consumer.ConsumerGroupMetadata");

      MethodHandles.Lookup lookup = MethodHandles.publicLookup();
      getGroupMetadata =
          lookup.findVirtual(
              Consumer.class, "groupMetadata", MethodType.methodType(consumerGroupMetadata));
      getGroupId =
          lookup.findVirtual(consumerGroupMetadata, "groupId", MethodType.methodType(String.class));
    } catch (ClassNotFoundException | IllegalAccessException | NoSuchMethodException ignored) {
      getGroupMetadata = null;
      getGroupId = null;
    }

    GET_GROUP_METADATA = getGroupMetadata;
    GET_GROUP_ID = getGroupId;
  }

  @Nullable
  public static String getConsumerGroup(Consumer<?, ?> consumer) {
    return getConsumerInfo(consumer).get(CONSUMER_GROUP);
  }

  @Nullable
  public static String getClientId(Consumer<?, ?> consumer) {
    return getConsumerInfo(consumer).get(CLIENT_ID);
  }

  private static Map<String, String> getConsumerInfo(Consumer<?, ?> consumer) {
    if (consumer == null) {
      return Collections.emptyMap();
    }
    Map<String, String> map = consumerInfoField.get(consumer);
    if (map == null) {
      map = new HashMap<>();
      map.put(CONSUMER_GROUP, extractConsumerGroup(consumer));
      map.put(CLIENT_ID, extractClientId(consumer));
      consumerInfoField.set(consumer, map);
    }
    return map;
  }

  @Nullable
  private static String extractConsumerGroup(Consumer<?, ?> consumer) {
    if (GET_GROUP_METADATA == null || GET_GROUP_ID == null) {
      return null;
    }
    if (consumer == null) {
      return null;
    }
    try {
      Object metadata = GET_GROUP_METADATA.invoke(consumer);
      return (String) GET_GROUP_ID.invoke(metadata);
    } catch (Throwable e) {
      return null;
    }
  }

  @Nullable
  private static String extractClientId(Consumer<?, ?> consumer) {
    try {
      Map<MetricName, ? extends Metric> metrics = consumer.metrics();
      Iterator<MetricName> metricIterator = metrics.keySet().iterator();
      return metricIterator.hasNext() ? metricIterator.next().tags().get("client-id") : null;
    } catch (RuntimeException exception) {
      // ExceptionHandlingTest uses a Consumer that throws exception on every method call
      return null;
    }
  }

  private KafkaUtil() {}
}
