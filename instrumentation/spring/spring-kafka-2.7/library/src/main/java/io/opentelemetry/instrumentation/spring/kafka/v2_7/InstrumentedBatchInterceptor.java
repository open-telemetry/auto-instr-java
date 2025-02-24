/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.spring.kafka.v2_7;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.util.VirtualField;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaConsumerContext;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaConsumerContextUtil;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaReceiveRequest;
import io.opentelemetry.javaagent.tooling.muzzle.NoMuzzle;
import java.lang.ref.WeakReference;
import javax.annotation.Nullable;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.kafka.listener.BatchInterceptor;

final class InstrumentedBatchInterceptor<K, V> implements BatchInterceptor<K, V> {

  private static final VirtualField<ConsumerRecords<?, ?>, State<KafkaReceiveRequest>> stateField =
      VirtualField.find(ConsumerRecords.class, State.class);
  private static final ThreadLocal<WeakReference<ConsumerRecords<?, ?>>> lastProcessed =
      new ThreadLocal<>();

  private final Instrumenter<KafkaReceiveRequest, Void> batchProcessInstrumenter;
  @Nullable private final BatchInterceptor<K, V> decorated;

  InstrumentedBatchInterceptor(
      Instrumenter<KafkaReceiveRequest, Void> batchProcessInstrumenter,
      @Nullable BatchInterceptor<K, V> decorated) {
    this.batchProcessInstrumenter = batchProcessInstrumenter;
    this.decorated = decorated;
  }

  @Override
  public ConsumerRecords<K, V> intercept(ConsumerRecords<K, V> records, Consumer<K, V> consumer) {
    Context parentContext = getParentContext(records);

    KafkaReceiveRequest request = KafkaReceiveRequest.create(records, consumer);
    if (batchProcessInstrumenter.shouldStart(parentContext, request) && !skipProcessing(records)) {
      Context context = batchProcessInstrumenter.start(parentContext, request);
      Scope scope = context.makeCurrent();
      stateField.set(records, State.create(request, context, scope));
    }

    return decorated == null ? records : decorated.intercept(records, consumer);
  }

  private static boolean skipProcessing(ConsumerRecords<?, ?> records) {
    // When retrying failed listener interceptors work as expected only in the earlier versions that
    // we test (e.g spring-kafka:2.7.1). In later versions interceptor isn't called at all during
    // the retry, which results in missing process span, or worse, the intercept method is called,
    // but neither success nor failure is called, which results in a context leak. Here we attempt
    // to prevent the context leak by observing whether intercept is called with the same
    // ConsumerRecords as on previous call, and if it is, we skip creating the process span.
    WeakReference<ConsumerRecords<?, ?>> reference = lastProcessed.get();
    return reference != null && reference.get() == records;
  }

  private static Context getParentContext(ConsumerRecords<?, ?> records) {
    KafkaConsumerContext consumerContext = KafkaConsumerContextUtil.get(records);
    Context receiveContext = consumerContext.getContext();

    // use the receive CONSUMER span as parent if it's available
    return receiveContext != null ? receiveContext : Context.current();
  }

  @Override
  public void success(ConsumerRecords<K, V> records, Consumer<K, V> consumer) {
    try {
      if (decorated != null) {
        decorated.success(records, consumer);
      }
    } finally {
      end(records, null);
    }
  }

  @Override
  public void failure(ConsumerRecords<K, V> records, Exception exception, Consumer<K, V> consumer) {
    try {
      if (decorated != null) {
        decorated.failure(records, exception, consumer);
      }
    } finally {
      end(records, exception);
    }
  }

  private void end(ConsumerRecords<K, V> records, @Nullable Throwable error) {
    State<KafkaReceiveRequest> state = stateField.get(records);
    stateField.set(records, null);
    if (state != null) {
      KafkaReceiveRequest request = state.request();
      state.scope().close();
      batchProcessInstrumenter.end(state.context(), request, null, error);
      lastProcessed.set(new WeakReference<>(records));
    }
  }

  @NoMuzzle // method was added in 2.8.0
  @Override
  public void setupThreadState(Consumer<?, ?> consumer) {
    if (decorated != null) {
      decorated.setupThreadState(consumer);
    }
  }

  @NoMuzzle // method was added in 2.8.0
  @Override
  public void clearThreadState(Consumer<?, ?> consumer) {
    if (decorated != null) {
      decorated.clearThreadState(consumer);
    }
  }
}
