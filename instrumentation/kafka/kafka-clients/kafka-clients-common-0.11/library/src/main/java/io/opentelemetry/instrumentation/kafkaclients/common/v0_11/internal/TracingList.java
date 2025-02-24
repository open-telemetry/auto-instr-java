/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal;

import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.function.BooleanSupplier;
import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public class TracingList<K, V> extends TracingIterable<K, V> implements List<ConsumerRecord<K, V>> {
  private final List<ConsumerRecord<K, V>> delegate;

  private TracingList(
      List<ConsumerRecord<K, V>> delegate,
      Instrumenter<KafkaProcessRequest, Void> instrumenter,
      BooleanSupplier wrappingEnabled,
      KafkaConsumerContext consumerContext) {
    super(delegate, instrumenter, wrappingEnabled, consumerContext);
    this.delegate = delegate;
  }

  public static <K, V> List<ConsumerRecord<K, V>> wrap(
      List<ConsumerRecord<K, V>> delegate,
      Instrumenter<KafkaProcessRequest, Void> instrumenter,
      BooleanSupplier wrappingEnabled,
      KafkaConsumerContext consumerContext) {
    if (wrappingEnabled.getAsBoolean()) {
      return new TracingList<>(delegate, instrumenter, wrappingEnabled, consumerContext);
    }
    return delegate;
  }

  @Override
  public int size() {
    return delegate.size();
  }

  @Override
  public boolean isEmpty() {
    return delegate.isEmpty();
  }

  @Override
  public boolean contains(Object o) {
    return delegate.contains(o);
  }

  @Override
  public Object[] toArray() {
    return delegate.toArray();
  }

  @Override
  public <T> T[] toArray(T[] a) {
    return delegate.toArray(a);
  }

  @Override
  public boolean add(ConsumerRecord<K, V> consumerRecord) {
    return delegate.add(consumerRecord);
  }

  @Override
  public void add(int index, ConsumerRecord<K, V> element) {
    delegate.add(index, element);
  }

  @Override
  public boolean remove(Object o) {
    return delegate.remove(o);
  }

  @Override
  public ConsumerRecord<K, V> remove(int index) {
    return delegate.remove(index);
  }

  @Override
  public boolean containsAll(Collection<?> c) {
    return delegate.containsAll(c);
  }

  @Override
  public boolean addAll(Collection<? extends ConsumerRecord<K, V>> c) {
    return delegate.addAll(c);
  }

  @Override
  public boolean addAll(int index, Collection<? extends ConsumerRecord<K, V>> c) {
    return delegate.addAll(index, c);
  }

  @Override
  public boolean removeAll(Collection<?> c) {
    return delegate.removeAll(c);
  }

  @Override
  public boolean retainAll(Collection<?> c) {
    return delegate.retainAll(c);
  }

  @Override
  public void clear() {
    delegate.clear();
  }

  @Override
  public ConsumerRecord<K, V> get(int index) {
    // TODO: should this be instrumented as well?
    return delegate.get(index);
  }

  @Override
  public ConsumerRecord<K, V> set(int index, ConsumerRecord<K, V> element) {
    return delegate.set(index, element);
  }

  @Override
  public int indexOf(Object o) {
    return delegate.indexOf(o);
  }

  @Override
  public int lastIndexOf(Object o) {
    return delegate.lastIndexOf(o);
  }

  @Override
  public ListIterator<ConsumerRecord<K, V>> listIterator() {
    // TODO: the API for ListIterator is not really good to instrument it in context of Kafka
    // Consumer so we will not do that for now
    return delegate.listIterator();
  }

  @Override
  public ListIterator<ConsumerRecord<K, V>> listIterator(int index) {
    // TODO: the API for ListIterator is not really good to instrument it in context of Kafka
    // Consumer so we will not do that for now
    return delegate.listIterator(index);
  }

  @Override
  public List<ConsumerRecord<K, V>> subList(int fromIndex, int toIndex) {
    // TODO: the API for subList is not really good to instrument it in context of Kafka
    // Consumer so we will not do that for now
    // Kafka is essentially a sequential commit log. We should only enable tracing when traversing
    // sequentially with an iterator
    return delegate.subList(fromIndex, toIndex);
  }
}
