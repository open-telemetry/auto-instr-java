/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.kafkaclients.v2_6;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.internal.ConfigPropertiesUtil;
import io.opentelemetry.instrumentation.api.internal.Timer;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaConsumerContext;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaConsumerContextUtil;
import java.util.Map;
import java.util.Objects;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerInterceptor;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;

/**
 * A ConsumerInterceptor that adds tracing capability. Add this interceptor's class name or class
 * via ConsumerConfig.INTERCEPTOR_CLASSES_CONFIG property to your Consumer's properties to get it
 * instantiated and used. See more details on ConsumerInterceptor usage in its Javadoc.
 */
public class TracingConsumerInterceptor<K, V> implements ConsumerInterceptor<K, V> {

  private static final KafkaTelemetry telemetry =
      KafkaTelemetry.builder(GlobalOpenTelemetry.get())
          .setMessagingReceiveInstrumentationEnabled(
              ConfigPropertiesUtil.getBoolean(
                  "otel.instrumentation.messaging.experimental.receive-telemetry.enabled", false))
          .build();

  private String consumerGroup;
  private String clientId;

  @Override
  @CanIgnoreReturnValue
  public ConsumerRecords<K, V> onConsume(ConsumerRecords<K, V> records) {
    // timer should be started before fetching ConsumerRecords, but there is no callback for that
    Timer timer = Timer.start();
    Context receiveContext = telemetry.buildAndFinishSpan(records, consumerGroup, clientId, timer);
    if (receiveContext == null) {
      receiveContext = Context.current();
    }
    KafkaConsumerContext consumerContext =
        KafkaConsumerContextUtil.create(receiveContext, consumerGroup, clientId);
    return telemetry.addTracing(records, consumerContext);
  }

  @Override
  public void onCommit(Map<TopicPartition, OffsetAndMetadata> offsets) {}

  @Override
  public void close() {}

  @Override
  public void configure(Map<String, ?> configs) {
    consumerGroup = Objects.toString(configs.get(ConsumerConfig.GROUP_ID_CONFIG), null);
    clientId = Objects.toString(configs.get(ConsumerConfig.CLIENT_ID_CONFIG), null);

    // TODO: support experimental attributes config
  }
}
