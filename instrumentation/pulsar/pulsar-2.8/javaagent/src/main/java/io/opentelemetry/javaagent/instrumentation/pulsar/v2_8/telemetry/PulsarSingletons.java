/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.pulsar.v2_8.telemetry;

import static io.opentelemetry.instrumentation.api.internal.SemconvStability.emitStableMessagingSemconv;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessageOperation;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesGetter;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingConsumerMetrics;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingNetworkAttributesExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingProducerMetrics;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingSpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanLinksExtractor;
import io.opentelemetry.instrumentation.api.internal.InstrumenterUtil;
import io.opentelemetry.instrumentation.api.internal.PropagatorBasedSpanLinksExtractor;
import io.opentelemetry.instrumentation.api.internal.Timer;
import io.opentelemetry.javaagent.bootstrap.internal.AgentInstrumentationConfig;
import io.opentelemetry.javaagent.bootstrap.internal.ExperimentalConfig;
import io.opentelemetry.javaagent.instrumentation.pulsar.v2_8.VirtualFieldStore;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.Messages;

public final class PulsarSingletons {
  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.pulsar-2.8";

  private static final OpenTelemetry TELEMETRY = GlobalOpenTelemetry.get();
  private static final TextMapPropagator PROPAGATOR =
      TELEMETRY.getPropagators().getTextMapPropagator();
  private static final List<String> capturedHeaders =
      ExperimentalConfig.get().getMessagingHeaders();
  private static final boolean receiveInstrumentationEnabled =
      ExperimentalConfig.get().messagingReceiveInstrumentationEnabled();

  private static final Instrumenter<PulsarRequest, Void> CONSUMER_PROCESS_INSTRUMENTER =
      createConsumerProcessInstrumenter();
  private static final Instrumenter<PulsarRequest, Void> CONSUMER_RECEIVE_INSTRUMENTER =
      createConsumerReceiveInstrumenter();
  private static final Instrumenter<PulsarBatchRequest, Void> CONSUMER_BATCH_RECEIVE_INSTRUMENTER =
      createConsumerBatchReceiveInstrumenter();
  private static final Instrumenter<PulsarRequest, Void> PRODUCER_INSTRUMENTER =
      createProducerInstrumenter();

  public static Instrumenter<PulsarRequest, Void> consumerProcessInstrumenter() {
    return CONSUMER_PROCESS_INSTRUMENTER;
  }

  public static Instrumenter<PulsarRequest, Void> consumerReceiveInstrumenter() {
    return CONSUMER_RECEIVE_INSTRUMENTER;
  }

  public static Instrumenter<PulsarRequest, Void> producerInstrumenter() {
    return PRODUCER_INSTRUMENTER;
  }

  private static Instrumenter<PulsarRequest, Void> createConsumerReceiveInstrumenter() {
    PulsarMessagingAttributesGetter getter = PulsarMessagingAttributesGetter.INSTANCE;

    InstrumenterBuilder<PulsarRequest, Void> instrumenterBuilder =
        Instrumenter.<PulsarRequest, Void>builder(
                TELEMETRY,
                INSTRUMENTATION_NAME,
                MessagingSpanNameExtractor.create(getter, MessageOperation.RECEIVE, getter))
            .addAttributesExtractor(
                createMessagingAttributesExtractor(getter, MessageOperation.RECEIVE))
            .addOperationMetrics(MessagingConsumerMetrics.get())
            .addAttributesExtractor(MessagingNetworkAttributesExtractor.create(getter));

    if (receiveInstrumentationEnabled || emitStableMessagingSemconv()) {
      return instrumenterBuilder
          .addSpanLinksExtractor(
              new PropagatorBasedSpanLinksExtractor<>(PROPAGATOR, MessageTextMapGetter.INSTANCE))
          .buildInstrumenter(SpanKindExtractor.alwaysConsumer());
    }
    return instrumenterBuilder.buildConsumerInstrumenter(MessageTextMapGetter.INSTANCE);
  }

  private static Instrumenter<PulsarBatchRequest, Void> createConsumerBatchReceiveInstrumenter() {
    PulsarBatchMessagingAttributesGetter getter = PulsarBatchMessagingAttributesGetter.INSTANCE;

    return Instrumenter.<PulsarBatchRequest, Void>builder(
            TELEMETRY,
            INSTRUMENTATION_NAME,
            MessagingSpanNameExtractor.create(getter, MessageOperation.RECEIVE, getter))
        .addAttributesExtractor(
            createMessagingAttributesExtractor(getter, MessageOperation.RECEIVE))
        .addAttributesExtractor(MessagingNetworkAttributesExtractor.create(getter))
        .addSpanLinksExtractor(new PulsarBatchRequestSpanLinksExtractor(PROPAGATOR))
        .addOperationMetrics(MessagingConsumerMetrics.get())
        .buildInstrumenter(SpanKindExtractor.alwaysConsumer());
  }

  private static Instrumenter<PulsarRequest, Void> createConsumerProcessInstrumenter() {
    PulsarMessagingAttributesGetter getter = PulsarMessagingAttributesGetter.INSTANCE;

    InstrumenterBuilder<PulsarRequest, Void> instrumenterBuilder =
        Instrumenter.<PulsarRequest, Void>builder(
                TELEMETRY,
                INSTRUMENTATION_NAME,
                MessagingSpanNameExtractor.create(getter, MessageOperation.PROCESS, getter))
            .addAttributesExtractor(
                createMessagingAttributesExtractor(getter, MessageOperation.PROCESS))
            .addAttributesExtractor(MessagingNetworkAttributesExtractor.create(getter));

    if (receiveInstrumentationEnabled || emitStableMessagingSemconv()) {
      SpanLinksExtractor<PulsarRequest> spanLinksExtractor =
          new PropagatorBasedSpanLinksExtractor<>(PROPAGATOR, MessageTextMapGetter.INSTANCE);
      instrumenterBuilder.addSpanLinksExtractor(spanLinksExtractor);
      return instrumenterBuilder.buildInstrumenter(SpanKindExtractor.alwaysConsumer());
    }
    return instrumenterBuilder.buildConsumerInstrumenter(MessageTextMapGetter.INSTANCE);
  }

  @SuppressWarnings("deprecation") // using deprecated semconv
  private static Instrumenter<PulsarRequest, Void> createProducerInstrumenter() {
    PulsarMessagingAttributesGetter getter = PulsarMessagingAttributesGetter.INSTANCE;
    MessageOperation operation =
        emitStableMessagingSemconv() ? MessageOperation.SEND : MessageOperation.PUBLISH;

    InstrumenterBuilder<PulsarRequest, Void> builder =
        Instrumenter.<PulsarRequest, Void>builder(
                TELEMETRY,
                INSTRUMENTATION_NAME,
                MessagingSpanNameExtractor.create(getter, operation, getter))
            .addAttributesExtractor(createMessagingAttributesExtractor(getter, operation))
            .addAttributesExtractor(MessagingNetworkAttributesExtractor.create(getter))
            .addOperationMetrics(MessagingProducerMetrics.get());

    if (AgentInstrumentationConfig.get()
        .getBoolean("otel.instrumentation.pulsar.experimental-span-attributes", false)) {
      builder.addAttributesExtractor(ExperimentalProducerAttributesExtractor.INSTANCE);
    }

    return builder.buildProducerInstrumenter(MessageTextMapSetter.INSTANCE);
  }

  private static <T> AttributesExtractor<T, Void> createMessagingAttributesExtractor(
      MessagingAttributesGetter<T, Void> getter, MessageOperation operation) {
    return MessagingAttributesExtractor.builder(getter, operation)
        .setCapturedHeaders(capturedHeaders)
        .build();
  }

  public static Context startAndEndConsumerReceive(
      Context parent, Message<?> message, Timer timer, Consumer<?> consumer, Throwable throwable) {
    if (message == null) {
      return null;
    }
    String brokerUrl = VirtualFieldStore.extract(consumer);
    PulsarRequest request = PulsarRequest.create(message, brokerUrl);
    if (!CONSUMER_RECEIVE_INSTRUMENTER.shouldStart(parent, request)) {
      return null;
    }
    if (!(receiveInstrumentationEnabled || emitStableMessagingSemconv())) {
      // suppress receive span when receive telemetry is not enabled and message is going to be
      // processed by a listener
      if (MessageListenerContext.isProcessing()) {
        return null;
      }
      parent = PROPAGATOR.extract(parent, request, MessageTextMapGetter.INSTANCE);
    }
    return InstrumenterUtil.startAndEnd(
        CONSUMER_RECEIVE_INSTRUMENTER,
        parent,
        request,
        null,
        throwable,
        timer.startTime(),
        timer.now());
  }

  private static Context startAndEndConsumerReceive(
      Context parent,
      Messages<?> messages,
      Timer timer,
      Consumer<?> consumer,
      Throwable throwable) {
    if (messages == null || messages.size() == 0) {
      return null;
    }
    String brokerUrl = VirtualFieldStore.extract(consumer);
    PulsarBatchRequest request = PulsarBatchRequest.create(messages, brokerUrl);
    if (!CONSUMER_BATCH_RECEIVE_INSTRUMENTER.shouldStart(parent, request)) {
      return null;
    }
    return InstrumenterUtil.startAndEnd(
        CONSUMER_BATCH_RECEIVE_INSTRUMENTER,
        parent,
        request,
        null,
        throwable,
        timer.startTime(),
        timer.now());
  }

  public static CompletableFuture<Void> wrap(CompletableFuture<Void> future) {
    Context parent = Context.current();
    CompletableFuture<Void> result = new CompletableFuture<>();
    future.whenComplete(
        (unused, t) ->
            runWithContext(
                parent,
                () -> {
                  if (t != null) {
                    result.completeExceptionally(t);
                  } else {
                    result.complete(null);
                  }
                }));

    return result;
  }

  public static CompletableFuture<Message<?>> wrap(
      CompletableFuture<Message<?>> future, Timer timer, Consumer<?> consumer) {
    boolean listenerContextActive = MessageListenerContext.isProcessing();
    Context parent = Context.current();
    CompletableFuture<Message<?>> result = new CompletableFuture<>();
    future.whenComplete(
        (message, throwable) -> {
          // we create a "receive" span when receive telemetry is enabled or when we know that
          // this message will not be passed to a listener that would create the "process" span
          Context context =
              receiveInstrumentationEnabled
                      || emitStableMessagingSemconv()
                      || !listenerContextActive
                  ? startAndEndConsumerReceive(parent, message, timer, consumer, throwable)
                  : parent;
          runWithContext(
              context,
              () -> {
                if (throwable != null) {
                  result.completeExceptionally(throwable);
                } else {
                  result.complete(message);
                }
              });
        });

    return result;
  }

  public static CompletableFuture<Messages<?>> wrapBatch(
      CompletableFuture<Messages<?>> future, Timer timer, Consumer<?> consumer) {
    Context parent = Context.current();
    CompletableFuture<Messages<?>> result = new CompletableFuture<>();
    future.whenComplete(
        (messages, throwable) -> {
          Context context =
              startAndEndConsumerReceive(parent, messages, timer, consumer, throwable);
          runWithContext(
              context,
              () -> {
                if (throwable != null) {
                  result.completeExceptionally(throwable);
                } else {
                  result.complete(messages);
                }
              });
        });

    return result;
  }

  private static void runWithContext(Context context, Runnable runnable) {
    if (context != null) {
      try (Scope ignored = context.makeCurrent()) {
        runnable.run();
      }
    } else {
      runnable.run();
    }
  }

  private PulsarSingletons() {}
}
