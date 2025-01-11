/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.rabbitmq;

import static io.opentelemetry.api.trace.SpanKind.PRODUCER;

import com.rabbitmq.client.GetResponse;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessageOperation;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesGetter;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor;
import io.opentelemetry.instrumentation.api.internal.PropagatorBasedSpanLinksExtractor;
import io.opentelemetry.instrumentation.api.semconv.network.NetworkAttributesExtractor;
import java.util.ArrayList;
import java.util.List;

public final class RabbitTelemetry {

  static final AttributeKey<String> RABBITMQ_COMMAND = AttributeKey.stringKey("rabbitmq.command");

  public static RabbitTelemetry create(OpenTelemetry openTelemetry) {
    return builder(openTelemetry).build();
  }

  public static RabbitTelemetryBuilder builder(OpenTelemetry openTelemetry) {
    return new RabbitTelemetryBuilder(openTelemetry);
  }

  private final Instrumenter<InstrumentedChannel, Void> channelInstrumenter;
  private final Instrumenter<ReceiveRequest, GetResponse> receiveInstrumenter;
  private final Instrumenter<DeliveryRequest, Void> deliverInstrumenter;

  RabbitTelemetry(
      OpenTelemetry openTelemetry,
      List<String> capturedHeaders,
      boolean captureExperimentalSpanAttributes) {
    channelInstrumenter = createChannelInstrumenter(openTelemetry, capturedHeaders);

    receiveInstrumenter =
        createReceiveInstrumenter(
            openTelemetry, capturedHeaders, captureExperimentalSpanAttributes);
    deliverInstrumenter =
        createDeliverInstrumenter(
            openTelemetry, capturedHeaders, captureExperimentalSpanAttributes);
  }

  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.rabbitmq-2.7";

  static final ContextKey<MessageHeadersHolder> MESSAGE_HEADERS_CONTEXT_KEY =
      ContextKey.named("opentelemetry-rabbitmq-message-headers-context-key");

  private static Instrumenter<InstrumentedChannel, Void> createChannelInstrumenter(
      OpenTelemetry openTelemetry, List<String> capturedHeaders) {
    return Instrumenter.<InstrumentedChannel, Void>builder(
            openTelemetry, INSTRUMENTATION_NAME, InstrumentedChannel::spanName)
        .addAttributesExtractor(
            buildMessagingAttributesExtractor(
                RabbitChannelAttributesGetter.INSTANCE, MessageOperation.PUBLISH, capturedHeaders))
        .addAttributesExtractor(
            NetworkAttributesExtractor.create(new RabbitChannelNetAttributesGetter()))
        .addContextCustomizer(
            (context, request, startAttributes) ->
                context.with(MESSAGE_HEADERS_CONTEXT_KEY, new MessageHeadersHolder()))
        .buildInstrumenter(channelAndMethod -> PRODUCER);
  }

  private static Instrumenter<ReceiveRequest, GetResponse> createReceiveInstrumenter(
      OpenTelemetry openTelemetry,
      List<String> capturedHeaders,
      boolean captureExperimentalSpanAttributes) {

    List<AttributesExtractor<ReceiveRequest, GetResponse>> extractors = new ArrayList<>();
    extractors.add(
        buildMessagingAttributesExtractor(
            RabbitReceiveAttributesGetter.INSTANCE, MessageOperation.RECEIVE, capturedHeaders));
    extractors.add(NetworkAttributesExtractor.create(new RabbitReceiveNetAttributesGetter()));
    if (captureExperimentalSpanAttributes) {
      extractors.add(new RabbitReceiveExperimentalAttributesExtractor());
    }

    return Instrumenter.<ReceiveRequest, GetResponse>builder(
            openTelemetry, INSTRUMENTATION_NAME, ReceiveRequest::spanName)
        .addAttributesExtractors(extractors)
        //        .setEnabled(ExperimentalConfig.get().messagingReceiveInstrumentationEnabled())
        .addSpanLinksExtractor(
            new PropagatorBasedSpanLinksExtractor<>(
                GlobalOpenTelemetry.getPropagators().getTextMapPropagator(),
                ReceiveRequestTextMapGetter.INSTANCE))
        .buildInstrumenter(SpanKindExtractor.alwaysConsumer());
  }

  private static Instrumenter<DeliveryRequest, Void> createDeliverInstrumenter(
      OpenTelemetry openTelemetry,
      List<String> capturedHeaders,
      boolean captureExperimentalSpanAttributes) {

    List<AttributesExtractor<DeliveryRequest, Void>> extractors = new ArrayList<>();
    extractors.add(
        buildMessagingAttributesExtractor(
            RabbitDeliveryAttributesGetter.INSTANCE, MessageOperation.PROCESS, capturedHeaders));
    extractors.add(new RabbitDeliveryExtraAttributesExtractor());
    if (captureExperimentalSpanAttributes) {
      extractors.add(new RabbitDeliveryExperimentalAttributesExtractor());
    }

    return Instrumenter.<DeliveryRequest, Void>builder(
            openTelemetry, INSTRUMENTATION_NAME, DeliveryRequest::spanName)
        .addAttributesExtractors(extractors)
        .buildConsumerInstrumenter(DeliveryRequestGetter.INSTANCE);
  }

  private static <T, V> AttributesExtractor<T, V> buildMessagingAttributesExtractor(
      MessagingAttributesGetter<T, V> getter,
      MessageOperation operation,
      List<String> capturedHeaders) {
    return MessagingAttributesExtractor.builder(getter, operation)
        .setCapturedHeaders(capturedHeaders)
        .build();
  }

  public Instrumenter<InstrumentedChannel, Void> getChannelInstrumenter() {
    return channelInstrumenter;
  }

  public Instrumenter<ReceiveRequest, GetResponse> getReceiveInstrumenter() {
    return receiveInstrumenter;
  }

  public Instrumenter<DeliveryRequest, Void> getDeliverInstrumenter() {
    return deliverInstrumenter;
  }
}
