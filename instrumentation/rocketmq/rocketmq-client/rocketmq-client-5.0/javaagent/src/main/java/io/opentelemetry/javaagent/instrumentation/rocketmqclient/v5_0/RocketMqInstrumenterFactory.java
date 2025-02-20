/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.rocketmqclient.v5_0;

import static io.opentelemetry.instrumentation.api.internal.SemconvStability.emitStableMessagingSemconv;

import apache.rocketmq.v2.ReceiveMessageRequest;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.StatusCode;
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
import io.opentelemetry.instrumentation.api.instrumenter.SpanStatusExtractor;
import io.opentelemetry.instrumentation.api.internal.PropagatorBasedSpanLinksExtractor;
import java.util.List;
import org.apache.rocketmq.client.apis.consumer.ConsumeResult;
import org.apache.rocketmq.client.apis.message.MessageView;
import org.apache.rocketmq.client.java.impl.producer.SendReceiptImpl;
import org.apache.rocketmq.client.java.message.PublishingMessageImpl;

final class RocketMqInstrumenterFactory {
  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.rocketmq-client-5.0";

  private RocketMqInstrumenterFactory() {}

  @SuppressWarnings("deprecation") // using deprecated semconv
  public static Instrumenter<PublishingMessageImpl, SendReceiptImpl> createProducerInstrumenter(
      OpenTelemetry openTelemetry, List<String> capturedHeaders) {
    RocketMqProducerAttributeGetter getter = RocketMqProducerAttributeGetter.INSTANCE;
    MessageOperation operation =
        emitStableMessagingSemconv() ? MessageOperation.CREATE : MessageOperation.PUBLISH;
    ;

    AttributesExtractor<PublishingMessageImpl, SendReceiptImpl> attributesExtractor =
        buildMessagingAttributesExtractor(getter, operation, capturedHeaders);

    InstrumenterBuilder<PublishingMessageImpl, SendReceiptImpl> instrumenterBuilder =
        Instrumenter.<PublishingMessageImpl, SendReceiptImpl>builder(
                openTelemetry,
                INSTRUMENTATION_NAME,
                MessagingSpanNameExtractor.create(getter, operation, getter))
            .addAttributesExtractor(attributesExtractor)
            .addAttributesExtractor(MessagingNetworkAttributesExtractor.create(getter))
            .addAttributesExtractor(RocketMqProducerAttributeExtractor.INSTANCE);
    return instrumenterBuilder.buildProducerInstrumenter(MessageMapSetter.INSTANCE);
  }

  public static Instrumenter<ReceiveMessageRequest, List<MessageView>>
      createConsumerReceiveInstrumenter(
          OpenTelemetry openTelemetry, List<String> capturedHeaders, boolean enabled) {
    RocketMqConsumerReceiveAttributeGetter getter = RocketMqConsumerReceiveAttributeGetter.INSTANCE;
    MessageOperation operation = MessageOperation.RECEIVE;

    AttributesExtractor<ReceiveMessageRequest, List<MessageView>> attributesExtractor =
        buildMessagingAttributesExtractor(getter, operation, capturedHeaders);

    InstrumenterBuilder<ReceiveMessageRequest, List<MessageView>> instrumenterBuilder =
        Instrumenter.<ReceiveMessageRequest, List<MessageView>>builder(
                openTelemetry,
                INSTRUMENTATION_NAME,
                MessagingSpanNameExtractor.create(getter, operation, getter))
            .setEnabled(enabled)
            .addAttributesExtractor(attributesExtractor)
            .addAttributesExtractor(MessagingNetworkAttributesExtractor.create(getter))
            .addAttributesExtractor(RocketMqConsumerReceiveAttributeExtractor.INSTANCE);
    return instrumenterBuilder.buildInstrumenter(SpanKindExtractor.alwaysConsumer());
  }

  public static Instrumenter<MessageView, ConsumeResult> createConsumerProcessInstrumenter(
      OpenTelemetry openTelemetry,
      List<String> capturedHeaders,
      boolean receiveInstrumentationEnabled) {
    RocketMqConsumerProcessAttributeGetter getter = RocketMqConsumerProcessAttributeGetter.INSTANCE;
    MessageOperation operation = MessageOperation.PROCESS;

    AttributesExtractor<MessageView, ConsumeResult> attributesExtractor =
        buildMessagingAttributesExtractor(getter, operation, capturedHeaders);

    InstrumenterBuilder<MessageView, ConsumeResult> instrumenterBuilder =
        Instrumenter.<MessageView, ConsumeResult>builder(
                openTelemetry,
                INSTRUMENTATION_NAME,
                MessagingSpanNameExtractor.create(getter, operation, getter))
            .addAttributesExtractor(attributesExtractor)
            .addAttributesExtractor(MessagingNetworkAttributesExtractor.create(getter))
            .addAttributesExtractor(RocketMqConsumerProcessAttributeExtractor.INSTANCE)
            .setSpanStatusExtractor(
                (spanStatusBuilder, messageView, consumeResult, error) -> {
                  if (consumeResult == ConsumeResult.FAILURE) {
                    spanStatusBuilder.setStatus(StatusCode.ERROR);
                  } else {
                    SpanStatusExtractor.getDefault()
                        .extract(spanStatusBuilder, messageView, consumeResult, error);
                  }
                });

    if (receiveInstrumentationEnabled) {
      SpanLinksExtractor<MessageView> spanLinksExtractor =
          new PropagatorBasedSpanLinksExtractor<>(
              openTelemetry.getPropagators().getTextMapPropagator(), MessageMapGetter.INSTANCE);
      instrumenterBuilder.addSpanLinksExtractor(spanLinksExtractor);
      return instrumenterBuilder.buildInstrumenter(SpanKindExtractor.alwaysConsumer());
    }
    return instrumenterBuilder.buildConsumerInstrumenter(MessageMapGetter.INSTANCE);
  }

  private static <T, R> AttributesExtractor<T, R> buildMessagingAttributesExtractor(
      MessagingAttributesGetter<T, R> getter,
      MessageOperation operation,
      List<String> capturedHeaders) {
    return MessagingAttributesExtractor.builder(getter, operation)
        .setCapturedHeaders(capturedHeaders)
        .build();
  }
}
