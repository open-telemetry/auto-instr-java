/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.pulsar.v2_8.telemetry;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessageOperation;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesGetter;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingSpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor;
import io.opentelemetry.instrumentation.api.internal.InstrumenterUtil;
import io.opentelemetry.instrumentation.api.internal.Timer;
import io.opentelemetry.instrumentation.api.semconv.network.ServerAttributesExtractor;
import io.opentelemetry.javaagent.bootstrap.internal.ExperimentalConfig;
import io.opentelemetry.javaagent.bootstrap.internal.InstrumentationConfig;
import io.opentelemetry.javaagent.instrumentation.pulsar.v2_8.VirtualFieldStore;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.Messages;

public final class PulsarSingletons {
  public static final String INSTRUMENTATION_NAME = "io.opentelemetry.pulsar-2.8";

  public static final String METRICS_CONFIG_NAME =
      "otel.instrumentation.pulsar-clients-metrics.enabled";
  public static final String EXPERIMENTAL_SPAN_ATTRIBUTES_NAME =
      "otel.instrumentation.pulsar.experimental-span-attributes";

  public static final OpenTelemetry TELEMETRY = GlobalOpenTelemetry.get();
  private static final TextMapPropagator PROPAGATOR =
      TELEMETRY.getPropagators().getTextMapPropagator();
  private static final List<String> capturedHeaders =
      ExperimentalConfig.get().getMessagingHeaders();

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
    MessagingAttributesGetter<PulsarRequest, Void> getter =
        PulsarMessagingAttributesGetter.INSTANCE;

    return Instrumenter.<PulsarRequest, Void>builder(
            TELEMETRY,
            INSTRUMENTATION_NAME,
            MessagingSpanNameExtractor.create(getter, MessageOperation.RECEIVE))
        .addAttributesExtractor(
            createMessagingAttributesExtractor(getter, MessageOperation.RECEIVE))
        .addAttributesExtractor(
            ServerAttributesExtractor.create(new PulsarNetClientAttributesGetter()))
        .buildConsumerInstrumenter(MessageTextMapGetter.INSTANCE);
  }

  private static Instrumenter<PulsarBatchRequest, Void> createConsumerBatchReceiveInstrumenter() {
    MessagingAttributesGetter<PulsarBatchRequest, Void> getter =
        PulsarBatchMessagingAttributesGetter.INSTANCE;

    return Instrumenter.<PulsarBatchRequest, Void>builder(
            TELEMETRY,
            INSTRUMENTATION_NAME,
            MessagingSpanNameExtractor.create(getter, MessageOperation.RECEIVE))
        .addAttributesExtractor(
            createMessagingAttributesExtractor(getter, MessageOperation.RECEIVE))
        .addAttributesExtractor(
            ServerAttributesExtractor.create(new PulsarNetClientAttributesGetter()))
        .setEnabled(ExperimentalConfig.get().messagingReceiveInstrumentationEnabled())
        .addSpanLinksExtractor(
            new PulsarBatchRequestSpanLinksExtractor(
                GlobalOpenTelemetry.getPropagators().getTextMapPropagator()))
        .buildInstrumenter(SpanKindExtractor.alwaysConsumer());
  }

  private static Instrumenter<PulsarRequest, Void> createConsumerProcessInstrumenter() {
    MessagingAttributesGetter<PulsarRequest, Void> getter =
        PulsarMessagingAttributesGetter.INSTANCE;

    return Instrumenter.<PulsarRequest, Void>builder(
            TELEMETRY,
            INSTRUMENTATION_NAME,
            MessagingSpanNameExtractor.create(getter, MessageOperation.PROCESS))
        .addAttributesExtractor(
            createMessagingAttributesExtractor(getter, MessageOperation.PROCESS))
        .buildInstrumenter();
  }

  private static Instrumenter<PulsarRequest, Void> createProducerInstrumenter() {
    MessagingAttributesGetter<PulsarRequest, Void> getter =
        PulsarMessagingAttributesGetter.INSTANCE;

    InstrumenterBuilder<PulsarRequest, Void> builder =
        Instrumenter.<PulsarRequest, Void>builder(
                TELEMETRY,
                INSTRUMENTATION_NAME,
                MessagingSpanNameExtractor.create(getter, MessageOperation.PUBLISH))
            .addAttributesExtractor(
                createMessagingAttributesExtractor(getter, MessageOperation.PUBLISH))
            .addAttributesExtractor(
                ServerAttributesExtractor.create(new PulsarNetClientAttributesGetter()));

    if (InstrumentationConfig.get().getBoolean(EXPERIMENTAL_SPAN_ATTRIBUTES_NAME, false)) {
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
    // startAndEnd not supports extract trace context from carrier
    // start not supports custom startTime
    // extract trace context by using TEXT_MAP_PROPAGATOR here.
    return InstrumenterUtil.startAndEnd(
        CONSUMER_RECEIVE_INSTRUMENTER,
        PROPAGATOR.extract(parent, request, MessageTextMapGetter.INSTANCE),
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
    if (messages == null) {
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

  public static CompletableFuture<Message<?>> wrap(
      CompletableFuture<Message<?>> future, Timer timer, Consumer<?> consumer) {
    Context parent = Context.current();
    CompletableFuture<Message<?>> result = new CompletableFuture<>();
    future.whenComplete(
        (message, throwable) -> {
          Context context = startAndEndConsumerReceive(parent, message, timer, consumer, throwable);
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
