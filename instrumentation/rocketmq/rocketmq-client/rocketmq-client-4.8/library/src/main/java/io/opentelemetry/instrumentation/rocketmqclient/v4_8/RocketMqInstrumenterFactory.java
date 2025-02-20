/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.rocketmqclient.v4_8;

import static io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor.constant;
import static io.opentelemetry.instrumentation.api.internal.SemconvStability.emitStableMessagingSemconv;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessageOperation;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesGetter;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingNetworkAttributesExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingSpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanLinksExtractor;
import io.opentelemetry.instrumentation.api.internal.PropagatorBasedSpanLinksExtractor;
import java.util.List;
import org.apache.rocketmq.client.hook.SendMessageContext;
import org.apache.rocketmq.common.message.MessageExt;

class RocketMqInstrumenterFactory {

  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.rocketmq-client-4.8";

  // copied from MessagingIncubatingAttributes
  private static final AttributeKey<String> MESSAGING_OPERATION =
      AttributeKey.stringKey("messaging.operation");
  private static final AttributeKey<String> MESSAGING_SYSTEM =
      AttributeKey.stringKey("messaging.system");

  @SuppressWarnings("deprecation") // using deprecated semconv
  static Instrumenter<SendMessageContext, Void> createProducerInstrumenter(
      OpenTelemetry openTelemetry,
      List<String> capturedHeaders,
      boolean captureExperimentalSpanAttributes) {

    RocketMqProducerAttributeGetter getter = RocketMqProducerAttributeGetter.INSTANCE;
    MessageOperation operation =
        emitStableMessagingSemconv() ? MessageOperation.CREATE : MessageOperation.PUBLISH;

    InstrumenterBuilder<SendMessageContext, Void> instrumenterBuilder =
        Instrumenter.<SendMessageContext, Void>builder(
                openTelemetry,
                INSTRUMENTATION_NAME,
                MessagingSpanNameExtractor.create(getter, operation, getter))
            .addAttributesExtractor(
                buildMessagingAttributesExtractor(getter, operation, capturedHeaders))
            .addAttributesExtractor(MessagingNetworkAttributesExtractor.create(getter));
    if (captureExperimentalSpanAttributes) {
      instrumenterBuilder.addAttributesExtractor(
          RocketMqProducerExperimentalAttributeExtractor.INSTANCE);
    }

    return instrumenterBuilder.buildProducerInstrumenter(MapSetter.INSTANCE);
  }

  static RocketMqConsumerInstrumenter createConsumerInstrumenter(
      OpenTelemetry openTelemetry,
      List<String> capturedHeaders,
      boolean captureExperimentalSpanAttributes) {

    InstrumenterBuilder<Void, Void> batchReceiveInstrumenterBuilder =
        Instrumenter.<Void, Void>builder(
                openTelemetry, INSTRUMENTATION_NAME, RocketMqInstrumenterFactory::spanNameOnReceive)
            .addAttributesExtractor(constant(MESSAGING_SYSTEM, "rocketmq"))
            .addAttributesExtractor(constant(MESSAGING_OPERATION, "receive"));

    return new RocketMqConsumerInstrumenter(
        createProcessInstrumenter(
            openTelemetry, capturedHeaders, captureExperimentalSpanAttributes, false),
        createProcessInstrumenter(
            openTelemetry, capturedHeaders, captureExperimentalSpanAttributes, true),
        batchReceiveInstrumenterBuilder.buildInstrumenter(SpanKindExtractor.alwaysConsumer()));
  }

  private static Instrumenter<MessageExt, Void> createProcessInstrumenter(
      OpenTelemetry openTelemetry,
      List<String> capturedHeaders,
      boolean captureExperimentalSpanAttributes,
      boolean batch) {

    RocketMqConsumerAttributeGetter getter = RocketMqConsumerAttributeGetter.INSTANCE;
    MessageOperation operation = MessageOperation.PROCESS;

    InstrumenterBuilder<MessageExt, Void> builder =
        Instrumenter.builder(
            openTelemetry,
            INSTRUMENTATION_NAME,
            MessagingSpanNameExtractor.create(getter, operation, getter));

    builder.addAttributesExtractor(
        buildMessagingAttributesExtractor(getter, operation, capturedHeaders));
    builder.addAttributesExtractor(MessagingNetworkAttributesExtractor.create(getter));
    if (captureExperimentalSpanAttributes) {
      builder.addAttributesExtractor(RocketMqConsumerExperimentalAttributeExtractor.INSTANCE);
    }

    if (batch) {
      SpanLinksExtractor<MessageExt> spanLinksExtractor =
          new PropagatorBasedSpanLinksExtractor<>(
              openTelemetry.getPropagators().getTextMapPropagator(),
              TextMapExtractAdapter.INSTANCE);

      return builder
          .addSpanLinksExtractor(spanLinksExtractor)
          .buildInstrumenter(SpanKindExtractor.alwaysConsumer());
    } else {
      return builder.buildConsumerInstrumenter(TextMapExtractAdapter.INSTANCE);
    }
  }

  private static <T> AttributesExtractor<T, Void> buildMessagingAttributesExtractor(
      MessagingAttributesGetter<T, Void> getter,
      MessageOperation operation,
      List<String> capturedHeaders) {
    return MessagingAttributesExtractor.builder(getter, operation)
        .setCapturedHeaders(capturedHeaders)
        .build();
  }

  private static String spanNameOnReceive(Void unused) {
    return "multiple_sources receive";
  }

  private RocketMqInstrumenterFactory() {}
}
