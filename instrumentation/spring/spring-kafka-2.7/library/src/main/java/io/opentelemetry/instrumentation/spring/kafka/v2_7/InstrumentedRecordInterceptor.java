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
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaProcessRequest;
import io.opentelemetry.javaagent.tooling.muzzle.NoMuzzle;
import javax.annotation.Nullable;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.listener.RecordInterceptor;

final class InstrumentedRecordInterceptor<K, V> implements RecordInterceptor<K, V> {

  private static final VirtualField<ConsumerRecord<?, ?>, State<KafkaProcessRequest>> stateField =
      VirtualField.find(ConsumerRecord.class, State.class);

  private final Instrumenter<KafkaProcessRequest, Void> processInstrumenter;
  @Nullable private final RecordInterceptor<K, V> decorated;

  InstrumentedRecordInterceptor(
      Instrumenter<KafkaProcessRequest, Void> processInstrumenter,
      @Nullable RecordInterceptor<K, V> decorated) {
    this.processInstrumenter = processInstrumenter;
    this.decorated = decorated;
  }

  @NoMuzzle
  @SuppressWarnings(
      "deprecation") // implementing deprecated method (removed in 3.0) for better compatibility
  @Override
  public ConsumerRecord<K, V> intercept(ConsumerRecord<K, V> record) {
    start(record, null);
    return decorated == null ? record : decorated.intercept(record);
  }

  @Override
  public ConsumerRecord<K, V> intercept(ConsumerRecord<K, V> record, Consumer<K, V> consumer) {
    start(record, consumer);
    return decorated == null ? record : decorated.intercept(record, consumer);
  }

  private void start(ConsumerRecord<K, V> record, Consumer<K, V> consumer) {
    Context parentContext = getParentContext(record);

    KafkaProcessRequest request = KafkaProcessRequest.create(record, consumer);
    if (processInstrumenter.shouldStart(parentContext, request)) {
      Context context = processInstrumenter.start(parentContext, request);
      Scope scope = context.makeCurrent();
      stateField.set(record, State.create(request, context, scope));
    }
  }

  private static Context getParentContext(ConsumerRecord<?, ?> record) {
    KafkaConsumerContext consumerContext = KafkaConsumerContextUtil.get(record);
    Context receiveContext = consumerContext.getContext();

    // use the receive CONSUMER span as parent if it's available
    return receiveContext != null ? receiveContext : Context.current();
  }

  @Override
  public void success(ConsumerRecord<K, V> record, Consumer<K, V> consumer) {
    try {
      if (decorated != null) {
        decorated.success(record, consumer);
      }
    } finally {
      end(record, null);
    }
  }

  @Override
  public void failure(ConsumerRecord<K, V> record, Exception exception, Consumer<K, V> consumer) {
    try {
      if (decorated != null) {
        decorated.failure(record, exception, consumer);
      }
    } finally {
      end(record, exception);
    }
  }

  private void end(ConsumerRecord<K, V> record, @Nullable Throwable error) {
    State<KafkaProcessRequest> state = stateField.get(record);
    stateField.set(record, null);
    if (state != null) {
      KafkaProcessRequest request = state.request();
      state.scope().close();
      processInstrumenter.end(state.context(), request, null, error);
    }
  }

  @NoMuzzle // method was added in 2.8.0
  @Override
  public void afterRecord(ConsumerRecord<K, V> record, Consumer<K, V> consumer) {
    if (decorated != null) {
      decorated.afterRecord(record, consumer);
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
