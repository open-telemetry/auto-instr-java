/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v2_2;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanLinksExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.messaging.MessageOperation;
import io.opentelemetry.instrumentation.api.instrumenter.messaging.MessagingAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.messaging.MessagingAttributesGetter;
import io.opentelemetry.instrumentation.api.instrumenter.messaging.MessagingSpanNameExtractor;
import io.opentelemetry.instrumentation.messagehandler.MessageReceiver;
import java.util.Map;
import javax.annotation.Nullable;
import software.amazon.awssdk.core.SdkPojo;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

public class SqsMessageReceiver
    extends MessageReceiver<ReceiveMessageRequest, ReceiveMessageResponse> {
  private final OpenTelemetry openTelemetry;
  private final String destination;
  private final SqsClient sqsClient;

  public SqsMessageReceiver(OpenTelemetry openTelemetry, String destination, SqsClient sqsClient) {
    this.openTelemetry = openTelemetry;
    this.destination = destination;
    this.sqsClient = sqsClient;
  }

  @Override
  public ReceiveMessageResponse doReceive(ReceiveMessageRequest request) {
    return sqsClient.receiveMessage(request);
  }

  @Override
  protected Instrumenter<ReceiveMessageResponse, Void> getMessageInstrumenter() {
    return Instrumenter.<ReceiveMessageResponse, Void>builder(
            openTelemetry, "io.opentelemetry.aws-sdk-2.2", getSpanNameExtractor())
        .addAttributesExtractor(getAttributesExtractor())
        .addSpanLinksExtractor(getSpanLinksExtractor())
        .buildInstrumenter(getSpanKindExtractor());
  }

  protected SpanNameExtractor<ReceiveMessageResponse> getSpanNameExtractor() {
    return MessagingSpanNameExtractor.create(
        getMessageingAttributesGetter(), MessageOperation.RECEIVE);
  }

  protected MessagingAttributesGetter<ReceiveMessageResponse, Void>
      getMessageingAttributesGetter() {
    String destination = this.destination;

    return new MessagingAttributesGetter<ReceiveMessageResponse, Void>() {
      @Nullable
      @Override
      public String getSystem(ReceiveMessageResponse v) {
        return "AmazonSQS";
      }

      @Nullable
      @Override
      @SuppressWarnings({"deprecation"}) // Inheriting from interface
      public String getDestinationKind(ReceiveMessageResponse v) {
        return null;
      }

      @Nullable
      @Override
      public String getDestination(ReceiveMessageResponse v) {
        return destination;
      }

      @Override
      public boolean isTemporaryDestination(ReceiveMessageResponse v) {
        return false;
      }

      @Nullable
      @Override
      public String getConversationId(ReceiveMessageResponse v) {
        return null;
      }

      @Nullable
      @Override
      public Long getMessagePayloadSize(ReceiveMessageResponse v) {
        long total = 0;

        for (Message message : v.messages()) {
          total += message.body().length();
        }

        return total;
      }

      @Nullable
      @Override
      public Long getMessagePayloadCompressedSize(ReceiveMessageResponse v) {
        return null;
      }

      @Nullable
      @Override
      public String getMessageId(ReceiveMessageResponse request, Void v) {
        return null;
      }
    };
  }

  protected SpanKindExtractor<ReceiveMessageResponse> getSpanKindExtractor() {
    return SpanKindExtractor.alwaysConsumer();
  }

  protected AttributesExtractor<ReceiveMessageResponse, Void> getAttributesExtractor() {
    return MessagingAttributesExtractor.create(
        getMessageingAttributesGetter(), MessageOperation.RECEIVE);
  }

  protected SpanLinksExtractor<ReceiveMessageResponse> getSpanLinksExtractor() {
    return (spanLinks, parentContext, response) -> {
      TextMapPropagator messagingPropagator = openTelemetry.getPropagators().getTextMapPropagator();

      for (Message message : response.messages()) {
        Map<String, SdkPojo> messageAtributes = SqsMessageAccess.getMessageAttributes(message);

        Context context =
            SqsParentContext.ofMessageAttributes(messageAtributes, messagingPropagator);

        if (context == Context.root()) {
          context = SqsParentContext.ofSystemAttributes(SqsMessageAccess.getAttributes(message));
        }

        SpanContext messageSpanCtx = Span.fromContext(context).getSpanContext();

        if (messageSpanCtx.isValid()) {
          spanLinks.addLink(messageSpanCtx);
        }
      }
    };
  }
}
