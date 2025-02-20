/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.kafkaclients.v2_6;

import static java.util.logging.Level.WARNING;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.internal.InstrumenterUtil;
import io.opentelemetry.instrumentation.api.internal.Timer;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaConsumerContext;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaConsumerContextUtil;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaHeadersSetter;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaProcessRequest;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaProducerRequest;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaReceiveRequest;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaUtil;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.MetricsReporterList;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.OpenTelemetryMetricsReporter;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.OpenTelemetrySupplier;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.TracingList;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.function.BiFunction;
import java.util.logging.Logger;
import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.header.Headers;
import org.apache.kafka.common.metrics.MetricsReporter;

public final class KafkaTelemetry {
  private static final Logger logger = Logger.getLogger(KafkaTelemetry.class.getName());

  private static final TextMapSetter<Headers> SETTER = KafkaHeadersSetter.INSTANCE;

  private final OpenTelemetry openTelemetry;
  private final Instrumenter<KafkaProducerRequest, RecordMetadata> producerInstrumenter;
  private final Instrumenter<KafkaReceiveRequest, Void> consumerReceiveInstrumenter;
  private final Instrumenter<KafkaProcessRequest, Void> consumerProcessInstrumenter;
  private final boolean producerPropagationEnabled;

  KafkaTelemetry(
      OpenTelemetry openTelemetry,
      Instrumenter<KafkaProducerRequest, RecordMetadata> producerInstrumenter,
      Instrumenter<KafkaReceiveRequest, Void> consumerReceiveInstrumenter,
      Instrumenter<KafkaProcessRequest, Void> consumerProcessInstrumenter,
      boolean producerPropagationEnabled) {
    this.openTelemetry = openTelemetry;
    this.producerInstrumenter = producerInstrumenter;
    this.consumerReceiveInstrumenter = consumerReceiveInstrumenter;
    this.consumerProcessInstrumenter = consumerProcessInstrumenter;
    this.producerPropagationEnabled = producerPropagationEnabled;
  }

  /** Returns a new {@link KafkaTelemetry} configured with the given {@link OpenTelemetry}. */
  public static KafkaTelemetry create(OpenTelemetry openTelemetry) {
    return builder(openTelemetry).build();
  }

  /**
   * Returns a new {@link KafkaTelemetryBuilder} configured with the given {@link OpenTelemetry}.
   */
  public static KafkaTelemetryBuilder builder(OpenTelemetry openTelemetry) {
    return new KafkaTelemetryBuilder(openTelemetry);
  }

  private TextMapPropagator propagator() {
    return openTelemetry.getPropagators().getTextMapPropagator();
  }

  /** Returns a decorated {@link Producer} that emits spans for each sent message. */
  @SuppressWarnings("unchecked")
  public <K, V> Producer<K, V> wrap(Producer<K, V> producer) {
    return (Producer<K, V>)
        Proxy.newProxyInstance(
            KafkaTelemetry.class.getClassLoader(),
            new Class<?>[] {Producer.class},
            (proxy, method, args) -> {
              // Future<RecordMetadata> send(ProducerRecord<K, V> record)
              // Future<RecordMetadata> send(ProducerRecord<K, V> record, Callback callback)
              if ("send".equals(method.getName())
                  && method.getParameterCount() >= 1
                  && method.getParameterTypes()[0] == ProducerRecord.class) {
                ProducerRecord<K, V> record = (ProducerRecord<K, V>) args[0];
                Callback callback =
                    method.getParameterCount() >= 2
                            && method.getParameterTypes()[1] == Callback.class
                        ? (Callback) args[1]
                        : null;
                return buildAndInjectSpan(record, producer, callback, producer::send);
              }
              try {
                return method.invoke(producer, args);
              } catch (InvocationTargetException exception) {
                throw exception.getCause();
              }
            });
  }

  /** Returns a decorated {@link Consumer} that consumes spans for each received message. */
  @SuppressWarnings("unchecked")
  public <K, V> Consumer<K, V> wrap(Consumer<K, V> consumer) {
    return (Consumer<K, V>)
        Proxy.newProxyInstance(
            KafkaTelemetry.class.getClassLoader(),
            new Class<?>[] {Consumer.class},
            (proxy, method, args) -> {
              Object result;
              Timer timer = "poll".equals(method.getName()) ? Timer.start() : null;
              try {
                result = method.invoke(consumer, args);
              } catch (InvocationTargetException exception) {
                throw exception.getCause();
              }
              // ConsumerRecords<K, V> poll(long timeout)
              // ConsumerRecords<K, V> poll(Duration duration)
              if ("poll".equals(method.getName()) && result instanceof ConsumerRecords) {
                ConsumerRecords<K, V> consumerRecords = (ConsumerRecords<K, V>) result;
                Context receiveContext = buildAndFinishSpan(consumerRecords, consumer, timer);
                if (receiveContext == null) {
                  receiveContext = Context.current();
                }
                KafkaConsumerContext consumerContext =
                    KafkaConsumerContextUtil.create(receiveContext, consumer);
                result = addTracing(consumerRecords, consumerContext);
              }
              return result;
            });
  }

  <K, V> ConsumerRecords<K, V> addTracing(
      ConsumerRecords<K, V> consumerRecords, KafkaConsumerContext consumerContext) {
    if (consumerRecords.isEmpty()) {
      return consumerRecords;
    }

    Map<TopicPartition, List<ConsumerRecord<K, V>>> records = new LinkedHashMap<>();
    for (TopicPartition partition : consumerRecords.partitions()) {
      List<ConsumerRecord<K, V>> list = consumerRecords.records(partition);
      if (list != null && !list.isEmpty()) {
        list = TracingList.wrap(list, consumerProcessInstrumenter, () -> true, consumerContext);
      }
      records.put(partition, list);
    }
    return new ConsumerRecords<>(records);
  }

  /**
   * Produces a set of kafka client config properties (consumer or producer) to register a {@link
   * MetricsReporter} that records metrics to an {@code openTelemetry} instance. Add these resulting
   * properties to the configuration map used to initialize a {@link KafkaConsumer} or {@link
   * KafkaProducer}.
   *
   * <p>For producers:
   *
   * <pre>{@code
   * //    Map<String, Object> config = new HashMap<>();
   * //    config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, ...);
   * //    config.putAll(kafkaTelemetry.metricConfigProperties());
   * //    try (KafkaProducer<?, ?> producer = new KafkaProducer<>(config)) { ... }
   * }</pre>
   *
   * <p>For consumers:
   *
   * <pre>{@code
   * //    Map<String, Object> config = new HashMap<>();
   * //    config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, ...);
   * //    config.putAll(kafkaTelemetry.metricConfigProperties());
   * //    try (KafkaConsumer<?, ?> consumer = new KafkaConsumer<>(config)) { ... }
   * }</pre>
   *
   * @return the kafka client properties
   */
  public Map<String, ?> metricConfigProperties() {
    Map<String, Object> config = new HashMap<>();
    config.put(
        CommonClientConfigs.METRIC_REPORTER_CLASSES_CONFIG,
        MetricsReporterList.singletonList(OpenTelemetryMetricsReporter.class));
    config.put(
        OpenTelemetryMetricsReporter.CONFIG_KEY_OPENTELEMETRY_SUPPLIER,
        new OpenTelemetrySupplier(openTelemetry));
    config.put(
        OpenTelemetryMetricsReporter.CONFIG_KEY_OPENTELEMETRY_INSTRUMENTATION_NAME,
        KafkaTelemetryBuilder.INSTRUMENTATION_NAME);
    return Collections.unmodifiableMap(config);
  }

  /**
   * Build and inject span into record.
   *
   * @param record the producer record to inject span info.
   */
  <K, V> void buildAndInjectSpan(ProducerRecord<K, V> record, String clientId) {
    Context parentContext = Context.current();

    KafkaProducerRequest request = KafkaProducerRequest.create(record, clientId);
    if (!producerInstrumenter.shouldStart(parentContext, request)) {
      return;
    }

    Context context = producerInstrumenter.start(parentContext, request);
    if (producerPropagationEnabled) {
      try {
        propagator().inject(context, record.headers(), SETTER);
      } catch (Throwable t) {
        // it can happen if headers are read only (when record is sent second time)
        logger.log(WARNING, "failed to inject span context. sending record second time?", t);
      }
    }
    producerInstrumenter.end(context, request, null, null);
  }

  /**
   * Build and inject span into record.
   *
   * @param record the producer record to inject span info.
   * @param callback the producer send callback
   * @return send function's result
   */
  <K, V> Future<RecordMetadata> buildAndInjectSpan(
      ProducerRecord<K, V> record,
      Producer<K, V> producer,
      Callback callback,
      BiFunction<ProducerRecord<K, V>, Callback, Future<RecordMetadata>> sendFn) {
    Context parentContext = Context.current();

    KafkaProducerRequest request = KafkaProducerRequest.create(record, producer);
    if (!producerInstrumenter.shouldStart(parentContext, request)) {
      return sendFn.apply(record, callback);
    }

    Context context = producerInstrumenter.start(parentContext, request);
    propagator().inject(context, record.headers(), SETTER);

    try (Scope ignored = context.makeCurrent()) {
      return sendFn.apply(record, new ProducerCallback(callback, parentContext, context, request));
    }
  }

  private <K, V> Context buildAndFinishSpan(
      ConsumerRecords<K, V> records, Consumer<K, V> consumer, Timer timer) {
    return buildAndFinishSpan(
        records, KafkaUtil.getConsumerGroup(consumer), KafkaUtil.getClientId(consumer), timer);
  }

  <K, V> Context buildAndFinishSpan(
      ConsumerRecords<K, V> records, String consumerGroup, String clientId, Timer timer) {
    if (records.isEmpty()) {
      return null;
    }
    Context parentContext = Context.current();
    KafkaReceiveRequest request = KafkaReceiveRequest.create(records, consumerGroup, clientId);
    Context context = null;
    if (consumerReceiveInstrumenter.shouldStart(parentContext, request)) {
      context =
          InstrumenterUtil.startAndEnd(
              consumerReceiveInstrumenter,
              parentContext,
              request,
              null,
              null,
              timer.startTime(),
              timer.now());
    }

    // we're returning the context of the receive span so that process spans can use it as
    // parent context even though the span has ended
    // this is the suggested behavior according to the spec batch receive scenario:
    // https://github.com/open-telemetry/semantic-conventions/blob/main/docs/messaging/messaging-spans.md#batch-receiving
    return context;
  }

  private class ProducerCallback implements Callback {
    private final Callback callback;
    private final Context parentContext;
    private final Context context;
    private final KafkaProducerRequest request;

    public ProducerCallback(
        Callback callback, Context parentContext, Context context, KafkaProducerRequest request) {
      this.callback = callback;
      this.parentContext = parentContext;
      this.context = context;
      this.request = request;
    }

    @Override
    public void onCompletion(RecordMetadata metadata, Exception exception) {
      producerInstrumenter.end(context, request, metadata, exception);

      if (callback != null) {
        try (Scope ignored = parentContext.makeCurrent()) {
          callback.onCompletion(metadata, exception);
        }
      }
    }
  }
}
