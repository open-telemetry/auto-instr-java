/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal;

import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import java.util.Iterator;
import java.util.function.BooleanSupplier;
import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public class TracingIterable<K, V> implements Iterable<ConsumerRecord<K, V>> {
  private final Iterable<ConsumerRecord<K, V>> delegate;
  private final Instrumenter<KafkaProcessRequest, Void> instrumenter;
  private final BooleanSupplier wrappingEnabled;
  private final KafkaConsumerContext consumerContext;
  private boolean firstIterator = true;

  protected TracingIterable(
      Iterable<ConsumerRecord<K, V>> delegate,
      Instrumenter<KafkaProcessRequest, Void> instrumenter,
      BooleanSupplier wrappingEnabled,
      KafkaConsumerContext consumerContext) {
    this.delegate = delegate;
    this.instrumenter = instrumenter;
    this.wrappingEnabled = wrappingEnabled;
    this.consumerContext = consumerContext;
  }

  public static <K, V> Iterable<ConsumerRecord<K, V>> wrap(
      Iterable<ConsumerRecord<K, V>> delegate,
      Instrumenter<KafkaProcessRequest, Void> instrumenter,
      BooleanSupplier wrappingEnabled,
      KafkaConsumerContext consumerContext) {
    if (wrappingEnabled.getAsBoolean()) {
      return new TracingIterable<>(delegate, instrumenter, wrappingEnabled, consumerContext);
    }
    return delegate;
  }

  @Override
  public Iterator<ConsumerRecord<K, V>> iterator() {
    Iterator<ConsumerRecord<K, V>> it;
    // We should only return one iterator with tracing.
    // However, this is not thread-safe, but usually the first (hopefully only) traversal of
    // ConsumerRecords is performed in the same thread that called poll()
    if (firstIterator) {
      it =
          TracingIterator.wrap(delegate.iterator(), instrumenter, wrappingEnabled, consumerContext);
      firstIterator = false;
    } else {
      it = delegate.iterator();
    }

    return it;
  }
}
