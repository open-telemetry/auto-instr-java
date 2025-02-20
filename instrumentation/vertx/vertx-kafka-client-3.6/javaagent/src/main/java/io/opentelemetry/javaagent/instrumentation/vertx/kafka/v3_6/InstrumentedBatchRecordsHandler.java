/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.vertx.kafka.v3_6;

import static io.opentelemetry.javaagent.instrumentation.vertx.kafka.v3_6.VertxKafkaSingletons.batchProcessInstrumenter;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaConsumerContext;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaConsumerContextUtil;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaReceiveRequest;
import io.opentelemetry.javaagent.bootstrap.kafka.KafkaClientsConsumerProcessTracing;
import io.vertx.core.Handler;
import javax.annotation.Nullable;
import org.apache.kafka.clients.consumer.ConsumerRecords;

public final class InstrumentedBatchRecordsHandler<K, V> implements Handler<ConsumerRecords<K, V>> {

  @Nullable private final Handler<ConsumerRecords<K, V>> delegate;

  public InstrumentedBatchRecordsHandler(@Nullable Handler<ConsumerRecords<K, V>> delegate) {
    this.delegate = delegate;
  }

  @Override
  public void handle(ConsumerRecords<K, V> records) {
    KafkaConsumerContext consumerContext = KafkaConsumerContextUtil.get(records);
    Context receiveContext = consumerContext.getContext();
    // use the receive CONSUMER span as parent if it's available
    Context parentContext = receiveContext != null ? receiveContext : Context.current();

    KafkaReceiveRequest request = KafkaReceiveRequest.create(consumerContext, records);
    if (!batchProcessInstrumenter().shouldStart(parentContext, request)) {
      callDelegateHandler(records);
      return;
    }

    // the instrumenter iterates over records when adding links, we need to suppress that
    boolean previousWrappingEnabled = KafkaClientsConsumerProcessTracing.setEnabled(false);
    try {
      Context context = batchProcessInstrumenter().start(parentContext, request);
      try (Scope ignored = context.makeCurrent()) {
        callDelegateHandler(records);
      } catch (Throwable t) {
        batchProcessInstrumenter().end(context, request, null, t);
        throw t;
      }
      batchProcessInstrumenter().end(context, request, null, null);
    } finally {
      KafkaClientsConsumerProcessTracing.setEnabled(previousWrappingEnabled);
    }
  }

  private void callDelegateHandler(ConsumerRecords<K, V> records) {
    if (delegate != null) {
      delegate.handle(records);
    }
  }
}
