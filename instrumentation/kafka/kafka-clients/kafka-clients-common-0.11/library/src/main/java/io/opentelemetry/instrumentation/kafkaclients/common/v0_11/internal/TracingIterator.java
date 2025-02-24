/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import java.util.Iterator;
import java.util.function.BooleanSupplier;
import javax.annotation.Nullable;
import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public class TracingIterator<K, V> implements Iterator<ConsumerRecord<K, V>> {

  private final Iterator<ConsumerRecord<K, V>> delegateIterator;
  private final Instrumenter<KafkaProcessRequest, Void> instrumenter;
  private final BooleanSupplier wrappingEnabled;
  private final Context parentContext;
  private final KafkaConsumerContext consumerContext;

  /*
   * Note: this may potentially create problems if this iterator is used from different threads. But
   * at the moment we cannot do much about this.
   */
  @Nullable private KafkaProcessRequest currentRequest;
  @Nullable private Context currentContext;
  @Nullable private Scope currentScope;

  private TracingIterator(
      Iterator<ConsumerRecord<K, V>> delegateIterator,
      Instrumenter<KafkaProcessRequest, Void> instrumenter,
      BooleanSupplier wrappingEnabled,
      KafkaConsumerContext consumerContext) {
    this.delegateIterator = delegateIterator;
    this.instrumenter = instrumenter;
    this.wrappingEnabled = wrappingEnabled;

    Context receiveContext = consumerContext.getContext();
    // use the receive CONSUMER as parent if it's available
    this.parentContext = receiveContext != null ? receiveContext : Context.current();
    this.consumerContext = consumerContext;
  }

  public static <K, V> Iterator<ConsumerRecord<K, V>> wrap(
      Iterator<ConsumerRecord<K, V>> delegateIterator,
      Instrumenter<KafkaProcessRequest, Void> instrumenter,
      BooleanSupplier wrappingEnabled,
      KafkaConsumerContext consumerContext) {
    if (wrappingEnabled.getAsBoolean()) {
      return new TracingIterator<>(
          delegateIterator, instrumenter, wrappingEnabled, consumerContext);
    }
    return delegateIterator;
  }

  @Override
  public boolean hasNext() {
    closeScopeAndEndSpan();
    return delegateIterator.hasNext();
  }

  @Override
  public ConsumerRecord<K, V> next() {
    // in case they didn't call hasNext()...
    closeScopeAndEndSpan();

    // it's important not to suppress consumer span creation here using Instrumenter.shouldStart()
    // because this instrumentation can leak the context and so there may be a leaked consumer span
    // in the context, in which case it's important to overwrite the leaked span instead of
    // suppressing the correct span
    // (https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/1947)
    ConsumerRecord<K, V> next = delegateIterator.next();
    if (next != null && wrappingEnabled.getAsBoolean()) {
      currentRequest = KafkaProcessRequest.create(consumerContext, next);
      currentContext = instrumenter.start(parentContext, currentRequest);
      currentScope = currentContext.makeCurrent();
    }
    return next;
  }

  private void closeScopeAndEndSpan() {
    if (currentScope != null) {
      currentScope.close();
      instrumenter.end(currentContext, currentRequest, null, null);
      currentScope = null;
      currentRequest = null;
      currentContext = null;
    }
  }

  @Override
  public void remove() {
    delegateIterator.remove();
  }
}
