/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.reactor.kafka.v1_0;

import static io.opentelemetry.javaagent.instrumentation.reactor.kafka.v1_0.ReactorKafkaSingletons.processInstrumenter;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaConsumerContext;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaConsumerContextUtil;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaProcessRequest;
import io.opentelemetry.instrumentation.reactor.v3_1.ContextPropagationOperator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.reactivestreams.Subscription;
import reactor.core.CoreSubscriber;
import reactor.core.Scannable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxOperator;
import reactor.core.publisher.Operators;

final class InstrumentedKafkaFlux<R extends ConsumerRecord<?, ?>> extends FluxOperator<R, R> {

  InstrumentedKafkaFlux(Flux<R> source) {
    super(source);
  }

  @Override
  @SuppressWarnings("unchecked")
  public void subscribe(CoreSubscriber<? super R> actual) {
    source.subscribe(new InstrumentedSubscriber((CoreSubscriber<ConsumerRecord<?, ?>>) actual));
  }

  static final class InstrumentedSubscriber
      implements CoreSubscriber<ConsumerRecord<?, ?>>, Subscription, Scannable {

    private final CoreSubscriber<ConsumerRecord<?, ?>> actual;
    private final Context currentContext;
    private Subscription subscription;

    InstrumentedSubscriber(CoreSubscriber<ConsumerRecord<?, ?>> actual) {
      this.actual = actual;
      currentContext =
          ContextPropagationOperator.getOpenTelemetryContext(
              actual.currentContext(), Context.current());
    }

    @Override
    public void onSubscribe(Subscription s) {
      if (Operators.validate(this.subscription, s)) {
        this.subscription = s;

        actual.onSubscribe(this);
      }
    }

    @Override
    public reactor.util.context.Context currentContext() {
      return actual.currentContext();
    }

    @Override
    public void onNext(ConsumerRecord<?, ?> record) {
      KafkaConsumerContext consumerContext = KafkaConsumerContextUtil.get(record);
      Context receiveContext = consumerContext.getContext();
      // use the receive CONSUMER span as parent if it's available
      Context parentContext = receiveContext != null ? receiveContext : currentContext;

      KafkaProcessRequest request = KafkaProcessRequest.create(consumerContext, record);
      if (!processInstrumenter().shouldStart(parentContext, request)) {
        actual.onNext(record);
        return;
      }

      Context context = processInstrumenter().start(parentContext, request);
      try (Scope ignored = context.makeCurrent()) {
        actual.onNext(record);
      } catch (Throwable t) {
        processInstrumenter().end(context, request, null, t);
        throw t;
      }
      processInstrumenter().end(context, request, null, null);
    }

    @Override
    public void onError(Throwable throwable) {
      try (Scope ignored = currentContext.makeCurrent()) {
        actual.onError(throwable);
      }
    }

    @Override
    public void onComplete() {
      try (Scope ignored = currentContext.makeCurrent()) {
        actual.onComplete();
      }
    }

    @Override
    public void request(long l) {
      subscription.request(l);
    }

    @Override
    public void cancel() {
      subscription.cancel();
    }

    @SuppressWarnings("rawtypes") // that's how the method is defined
    @Override
    public Object scanUnsafe(Attr key) {
      if (key == Attr.ACTUAL) {
        return actual;
      }
      if (key == Attr.PARENT) {
        return subscription;
      }
      return null;
    }
  }
}
