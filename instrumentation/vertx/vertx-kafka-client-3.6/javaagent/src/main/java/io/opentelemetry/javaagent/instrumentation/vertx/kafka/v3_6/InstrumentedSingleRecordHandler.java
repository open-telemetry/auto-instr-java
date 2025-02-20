/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.vertx.kafka.v3_6;

import static io.opentelemetry.javaagent.instrumentation.vertx.kafka.v3_6.VertxKafkaSingletons.processInstrumenter;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaConsumerContext;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaConsumerContextUtil;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaProcessRequest;
import io.vertx.core.Handler;
import javax.annotation.Nullable;
import org.apache.kafka.clients.consumer.ConsumerRecord;

public final class InstrumentedSingleRecordHandler<K, V> implements Handler<ConsumerRecord<K, V>> {

  @Nullable private final Handler<ConsumerRecord<K, V>> delegate;

  public InstrumentedSingleRecordHandler(@Nullable Handler<ConsumerRecord<K, V>> delegate) {
    this.delegate = delegate;
  }

  @Override
  public void handle(ConsumerRecord<K, V> record) {
    KafkaConsumerContext consumerContext = KafkaConsumerContextUtil.get(record);
    Context receiveContext = consumerContext.getContext();
    // use the receive CONSUMER span as parent if it's available
    Context parentContext = receiveContext != null ? receiveContext : Context.current();

    KafkaProcessRequest request = KafkaProcessRequest.create(consumerContext, record);
    if (!processInstrumenter().shouldStart(parentContext, request)) {
      callDelegateHandler(record);
      return;
    }

    Context context = processInstrumenter().start(parentContext, request);
    try (Scope ignored = context.makeCurrent()) {
      callDelegateHandler(record);
    } catch (Throwable t) {
      processInstrumenter().end(context, request, null, t);
      throw t;
    }
    processInstrumenter().end(context, request, null, null);
  }

  private void callDelegateHandler(ConsumerRecord<K, V> record) {
    if (delegate != null) {
      delegate.handle(record);
    }
  }
}
