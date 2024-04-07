/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.pulsar.v2_8;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.javaagent.bootstrap.internal.InstrumentationConfig;
import io.opentelemetry.javaagent.instrumentation.pulsar.v2_8.telemetry.PulsarSingletons;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.pulsar.client.impl.ConsumerImpl;
import org.apache.pulsar.client.impl.ProducerImpl;

public class PulsarMetricsRegistry {

  private static final String PULSAR_CLIENT_PREFIX = "pulsarclient.";
  private static final String PRODUCER_METRICS_PREFIX = PULSAR_CLIENT_PREFIX + "producer.";
  private static final String CONSUMER_METRICS_PREFIX = PULSAR_CLIENT_PREFIX + "consumer.";
  private static final AttributeKey<String> ATTRIBUTE_TOPIC =
      AttributeKey.stringKey("pulsar.topic");
  private static final AttributeKey<String> ATTRIBUTE_SUBSCRIPTION =
      AttributeKey.stringKey("pulsar.subscription");
  private static final AttributeKey<String> ATTRIBUTE_PRODUCER_NAME =
      AttributeKey.stringKey("pulsar.producer.name");
  private static final AttributeKey<String> ATTRIBUTE_CONSUMER_NAME =
      AttributeKey.stringKey("pulsar.consumer.name");
  private static final AttributeKey<String> ATTRIBUTE_QUANTILE = AttributeKey.stringKey("quantile");
  private static final AttributeKey<String> ATTRIBUTE_RESPONSE_STATUS =
      AttributeKey.stringKey("pulsar.response.status");

  private final Set<ProducerImpl<?>> producers = Collections.synchronizedSet(new HashSet<>());
  private final Set<ConsumerImpl<?>> consumers = Collections.synchronizedSet(new HashSet<>());
  private final Meter meter =
      PulsarSingletons.TELEMETRY
          .getMeterProvider()
          .meterBuilder(PulsarSingletons.INSTRUMENTATION_NAME)
          .build();

  public void init() {

    /* =========================== Producer Metrics =========================== */

    // pulsar.client.producer.message.sent.size
    meter
        .gaugeBuilder(PRODUCER_METRICS_PREFIX + "message.send.size")
        .setUnit("By")
        .setDescription("Counts the size of sent messages")
        .ofLongs()
        .buildWithCallback(
            measurement -> {
              for (ProducerImpl<?> producer : PulsarMetricsRegistry.this.producers) {
                measurement.record(
                    producer.getStats().getTotalBytesSent(),
                    Attributes.of(
                        ATTRIBUTE_TOPIC,
                        producer.getTopic(),
                        ATTRIBUTE_PRODUCER_NAME,
                        producer.getProducerName()));
              }
            });

    // pulsar.client.producer.message.sent.count
    meter
        .gaugeBuilder(PRODUCER_METRICS_PREFIX + "message.send.count")
        .setUnit("message")
        .setDescription("Counts the number of sent messages")
        .ofLongs()
        .buildWithCallback(
            measurement -> {
              for (ProducerImpl<?> producer : PulsarMetricsRegistry.this.producers) {
                measurement.record(
                    producer.getStats().getTotalMsgsSent(),
                    Attributes.of(
                        ATTRIBUTE_TOPIC,
                        producer.getTopic(),
                        ATTRIBUTE_PRODUCER_NAME,
                        producer.getProducerName(),
                        ATTRIBUTE_RESPONSE_STATUS,
                        "success"));
                measurement.record(
                    producer.getStats().getTotalSendFailed(),
                    Attributes.of(
                        ATTRIBUTE_TOPIC,
                        producer.getTopic(),
                        ATTRIBUTE_PRODUCER_NAME,
                        producer.getProducerName(),
                        ATTRIBUTE_RESPONSE_STATUS,
                        "failure"));
              }
            });

    // pulsar.client.producer.message.sent.duration
    meter
        .gaugeBuilder(PRODUCER_METRICS_PREFIX + "message.send.duration")
        .setUnit("ms")
        .setDescription("The duration of sent messages")
        .buildWithCallback(
            measurement -> {
              for (ProducerImpl<?> producer : PulsarMetricsRegistry.this.producers) {
                measurement.record(
                    producer.getStats().getSendLatencyMillis50pct(),
                    Attributes.of(
                        ATTRIBUTE_TOPIC,
                        producer.getTopic(),
                        ATTRIBUTE_PRODUCER_NAME,
                        producer.getProducerName(),
                        ATTRIBUTE_QUANTILE,
                        "0.5"));
                measurement.record(
                    producer.getStats().getSendLatencyMillis75pct(),
                    Attributes.of(
                        ATTRIBUTE_TOPIC,
                        producer.getTopic(),
                        ATTRIBUTE_PRODUCER_NAME,
                        producer.getProducerName(),
                        ATTRIBUTE_QUANTILE,
                        "0.75"));
                measurement.record(
                    producer.getStats().getSendLatencyMillis95pct(),
                    Attributes.of(
                        ATTRIBUTE_TOPIC,
                        producer.getTopic(),
                        ATTRIBUTE_PRODUCER_NAME,
                        producer.getProducerName(),
                        ATTRIBUTE_QUANTILE,
                        "0.95"));
                measurement.record(
                    producer.getStats().getSendLatencyMillis99pct(),
                    Attributes.of(
                        ATTRIBUTE_TOPIC,
                        producer.getTopic(),
                        ATTRIBUTE_PRODUCER_NAME,
                        producer.getProducerName(),
                        ATTRIBUTE_QUANTILE,
                        "0.99"));
                measurement.record(
                    producer.getStats().getSendLatencyMillis999pct(),
                    Attributes.of(
                        ATTRIBUTE_TOPIC,
                        producer.getTopic(),
                        ATTRIBUTE_PRODUCER_NAME,
                        producer.getProducerName(),
                        ATTRIBUTE_QUANTILE,
                        "0.999"));
                measurement.record(
                    producer.getStats().getSendLatencyMillisMax(),
                    Attributes.of(
                        ATTRIBUTE_TOPIC,
                        producer.getTopic(),
                        ATTRIBUTE_PRODUCER_NAME,
                        producer.getProducerName(),
                        ATTRIBUTE_QUANTILE,
                        "max"));
              }
            });

    /* =========================== Consumer Metrics =========================== */

    // pulsar.client.consumer.message.received.size
    meter
        .gaugeBuilder(CONSUMER_METRICS_PREFIX + "message.received.size")
        .setUnit("By")
        .setDescription("Counts the size of received messages")
        .ofLongs()
        .buildWithCallback(
            measurement -> {
              for (ConsumerImpl<?> consumer : PulsarMetricsRegistry.this.consumers) {
                measurement.record(
                    consumer.getStats().getTotalBytesReceived(),
                    Attributes.of(
                        ATTRIBUTE_TOPIC,
                        consumer.getTopic(),
                        ATTRIBUTE_SUBSCRIPTION,
                        consumer.getSubscription(),
                        ATTRIBUTE_CONSUMER_NAME,
                        consumer.getConsumerName()));
              }
            });

    // pulsar.client.consumer.message.received.count
    meter
        .gaugeBuilder(CONSUMER_METRICS_PREFIX + "message.received.count")
        .setUnit("message")
        .setDescription("Counts the number of received messages")
        .ofLongs()
        .buildWithCallback(
            measurement -> {
              for (ConsumerImpl<?> consumer : PulsarMetricsRegistry.this.consumers) {
                measurement.record(
                    consumer.getStats().getTotalMsgsReceived(),
                    Attributes.of(
                        ATTRIBUTE_TOPIC,
                        consumer.getTopic(),
                        ATTRIBUTE_SUBSCRIPTION,
                        consumer.getSubscription(),
                        ATTRIBUTE_CONSUMER_NAME,
                        consumer.getConsumerName(),
                        ATTRIBUTE_RESPONSE_STATUS,
                        "success"));
                measurement.record(
                    consumer.getStats().getTotalReceivedFailed(),
                    Attributes.of(
                        ATTRIBUTE_TOPIC,
                        consumer.getTopic(),
                        ATTRIBUTE_SUBSCRIPTION,
                        consumer.getSubscription(),
                        ATTRIBUTE_CONSUMER_NAME,
                        consumer.getConsumerName(),
                        ATTRIBUTE_RESPONSE_STATUS,
                        "failure"));
              }
            });

    // pulsar.client.consumer.acks.sent.count
    meter
        .gaugeBuilder(CONSUMER_METRICS_PREFIX + "message.ack.count")
        .setUnit("ack")
        .setDescription("Counts the number of sent message acknowledgements")
        .ofLongs()
        .buildWithCallback(
            measurement -> {
              for (ConsumerImpl<?> consumer : PulsarMetricsRegistry.this.consumers) {
                measurement.record(
                    consumer.getStats().getTotalAcksSent(),
                    Attributes.of(
                        ATTRIBUTE_TOPIC,
                        consumer.getTopic(),
                        ATTRIBUTE_SUBSCRIPTION,
                        consumer.getSubscription(),
                        ATTRIBUTE_CONSUMER_NAME,
                        consumer.getConsumerName(),
                        ATTRIBUTE_RESPONSE_STATUS,
                        "success"));
                measurement.record(
                    consumer.getStats().getTotalAcksFailed(),
                    Attributes.of(
                        ATTRIBUTE_TOPIC,
                        consumer.getTopic(),
                        ATTRIBUTE_SUBSCRIPTION,
                        consumer.getSubscription(),
                        ATTRIBUTE_CONSUMER_NAME,
                        consumer.getConsumerName(),
                        ATTRIBUTE_RESPONSE_STATUS,
                        "failure"));
              }
            });

    // pulsar.client.consumer.receiver.queue.usage
    meter
        .gaugeBuilder(CONSUMER_METRICS_PREFIX + "receiver.queue.usage")
        .setUnit("message")
        .setDescription("Number of the messages in the receiver queue")
        .ofLongs()
        .buildWithCallback(
            measurement -> {
              for (ConsumerImpl<?> consumer : PulsarMetricsRegistry.this.consumers) {
                measurement.record(
                    consumer.getTotalIncomingMessages(),
                    Attributes.of(
                        ATTRIBUTE_TOPIC,
                        consumer.getTopic(),
                        ATTRIBUTE_SUBSCRIPTION,
                        consumer.getSubscription(),
                        ATTRIBUTE_CONSUMER_NAME,
                        consumer.getConsumerName()));
              }
            });

    // TODO Add receiver queue limit metrics after the value is exposed by Pulsar Client.
  }

  private static volatile PulsarMetricsRegistry metricsRegistry;

  public static PulsarMetricsRegistry getMetricsRegistry() {
    if (metricsRegistry == null) {
      synchronized (PulsarMetricsRegistry.class) {
        if (metricsRegistry == null) {
          if (InstrumentationConfig.get().getBoolean(PulsarSingletons.METRICS_CONFIG_NAME, true)) {
            metricsRegistry = new PulsarMetricsRegistry();
            metricsRegistry.init();
          } else {
            metricsRegistry = PulsarMetricsRegistry.PulsarMetricsRegistryDisabled.INSTANCE;
          }
        }
      }
    }
    return metricsRegistry;
  }

  public void registerProducer(ProducerImpl<?> producer) {
    producers.add(producer);
  }

  public void registerConsumer(ConsumerImpl<?> consumer) {
    consumers.add(consumer);
  }

  public void deleteProducer(ProducerImpl<?> producer) {
    producers.remove(producer);
  }

  public void deleteConsumer(ConsumerImpl<?> consumer) {
    consumers.remove(consumer);
  }

  public int getProducerSize() {
    return producers.size();
  }

  public int getConsumerSize() {
    return consumers.size();
  }

  public static class PulsarMetricsRegistryDisabled extends PulsarMetricsRegistry {

    public static final PulsarMetricsRegistry INSTANCE = new PulsarMetricsRegistryDisabled();

    @Override
    public void init() {}

    @Override
    public void registerProducer(ProducerImpl<?> producer) {}

    @Override
    public void registerConsumer(ConsumerImpl<?> consumer) {}

    @Override
    public void deleteProducer(ProducerImpl<?> producer) {}

    @Override
    public void deleteConsumer(ConsumerImpl<?> consumer) {}

    @Override
    public int getProducerSize() {
      return 0;
    }

    @Override
    public int getConsumerSize() {
      return 0;
    }
  }
}
