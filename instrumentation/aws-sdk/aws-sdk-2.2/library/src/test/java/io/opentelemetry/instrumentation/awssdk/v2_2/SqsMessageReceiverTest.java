/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v2_2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

public class SqsMessageReceiverTest extends XrayTestInstrumenter {
  @Test
  public void simple() {
    SqsClient sqsClient =
        getClient(
            ReceiveMessageResponse.builder()
                .messages(
                    Message.builder()
                        .body("Hello")
                        .attributesWithStrings(
                            Collections.singletonMap(
                                "AWSTraceHeader",
                                "Root=1-55555555-123456789012345678901234;Parent=1234567890123456;Sampled=1"))
                        .build())
                .build());

    ReceiveMessageRequest request = ReceiveMessageRequest.builder().build();

    SqsMessageReceiver messageHandler =
        new SqsMessageReceiver(getOpenTelemetry(), "destination", sqsClient);

    Span parentSpan = getOpenTelemetry().getTracer("test").spanBuilder("test").startSpan();

    try (Scope scope = parentSpan.makeCurrent()) {
      ReceiveMessageResponse response = messageHandler.receive(request);

      assertThat(response.messages().size()).isEqualTo(1);
      assertThat(response.messages().get(0).body()).isEqualTo("Hello");
      assertThat(response.messages().get(0).attributesAsStrings().get("AWSTraceHeader"))
          .isEqualTo("Root=1-55555555-123456789012345678901234;Parent=1234567890123456;Sampled=1");
    }

    parentSpan.end();

    waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("test").hasTotalAttributeCount(0).hasTotalRecordedLinks(0),
                span ->
                    span.hasName("destination receive")
                        .hasKind(SpanKind.CONSUMER)
                        .hasLinks(
                            LinkData.create(
                                SpanContext.createFromRemoteParent(
                                    "55555555123456789012345678901234",
                                    "1234567890123456",
                                    TraceFlags.getSampled(),
                                    TraceState.getDefault())))
                        .hasTotalRecordedLinks(1)
                        .hasAttribute(SemanticAttributes.MESSAGING_OPERATION, "receive")
                        .hasAttribute(SemanticAttributes.MESSAGING_SYSTEM, "AmazonSQS")
                        .hasAttribute(SemanticAttributes.MESSAGING_DESTINATION_NAME, "destination")
                        .hasAttribute(SemanticAttributes.MESSAGING_MESSAGE_PAYLOAD_SIZE_BYTES, 5L)
                        .hasTotalAttributeCount(4)
                        .hasParentSpanId(parentSpan.getSpanContext().getSpanId())
                        .hasTraceId(parentSpan.getSpanContext().getTraceId())));
  }

  @Test
  public void twoMessages() {
    SqsClient sqsClient =
        getClient(
            ReceiveMessageResponse.builder()
                .messages(
                    Message.builder()
                        .body("Hello")
                        .attributesWithStrings(
                            Collections.singletonMap(
                                "AWSTraceHeader",
                                "Root=1-55555555-123456789012345678901234;Parent=1234567890123456;Sampled=1"))
                        .build(),
                    Message.builder()
                        .body("Hello World")
                        .attributesWithStrings(
                            Collections.singletonMap(
                                "AWSTraceHeader",
                                "Root=1-66555555-123456789012345678901234;Parent=6634567890123456;Sampled=0"))
                        .build())
                .build());

    ReceiveMessageRequest request = ReceiveMessageRequest.builder().build();

    SqsMessageReceiver messageHandler =
        new SqsMessageReceiver(getOpenTelemetry(), "destination", sqsClient);

    Span parentSpan = getOpenTelemetry().getTracer("test").spanBuilder("test").startSpan();

    try (Scope scope = parentSpan.makeCurrent()) {
      ReceiveMessageResponse response = messageHandler.receive(request);
      assertThat(response.messages().size()).isEqualTo(2);

      assertThat(response.messages().get(0).body()).isEqualTo("Hello");
      assertThat(response.messages().get(0).attributesAsStrings().get("AWSTraceHeader"))
          .isEqualTo("Root=1-55555555-123456789012345678901234;Parent=1234567890123456;Sampled=1");

      assertThat(response.messages().get(1).body()).isEqualTo("Hello World");
      assertThat(response.messages().get(1).attributesAsStrings().get("AWSTraceHeader"))
          .isEqualTo("Root=1-66555555-123456789012345678901234;Parent=6634567890123456;Sampled=0");
    }

    parentSpan.end();

    waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("test").hasTotalAttributeCount(0).hasTotalRecordedLinks(0),
                span ->
                    span.hasName("destination receive")
                        .hasKind(SpanKind.CONSUMER)
                        .hasLinks(
                            LinkData.create(
                                SpanContext.createFromRemoteParent(
                                    "55555555123456789012345678901234",
                                    "1234567890123456",
                                    TraceFlags.getSampled(),
                                    TraceState.getDefault())),
                            LinkData.create(
                                SpanContext.createFromRemoteParent(
                                    "66555555123456789012345678901234",
                                    "6634567890123456",
                                    TraceFlags.getDefault(),
                                    TraceState.getDefault())))
                        .hasTotalRecordedLinks(2)
                        .hasAttribute(SemanticAttributes.MESSAGING_OPERATION, "receive")
                        .hasAttribute(SemanticAttributes.MESSAGING_SYSTEM, "AmazonSQS")
                        .hasAttribute(SemanticAttributes.MESSAGING_DESTINATION_NAME, "destination")
                        .hasAttribute(SemanticAttributes.MESSAGING_MESSAGE_PAYLOAD_SIZE_BYTES, 16L)
                        .hasTotalAttributeCount(4)
                        .hasParentSpanId(parentSpan.getSpanContext().getSpanId())
                        .hasTraceId(parentSpan.getSpanContext().getTraceId())));
  }

  @Test
  public void twoRuns() {
    SqsClient sqsClient =
        getClient(
            ReceiveMessageResponse.builder()
                .messages(
                    Message.builder()
                        .body("Hello")
                        .attributesWithStrings(
                            Collections.singletonMap(
                                "AWSTraceHeader",
                                "Root=1-55555555-123456789012345678901234;Parent=1234567890123456;Sampled=0"))
                        .build())
                .build(),
            ReceiveMessageResponse.builder()
                .messages(
                    Message.builder()
                        .body("SecondMessage")
                        .attributesWithStrings(
                            Collections.singletonMap(
                                "AWSTraceHeader",
                                "Root=1-77555555-123456789012345678901234;Parent=7734567890123456;Sampled=1"))
                        .build())
                .build());

    ReceiveMessageRequest request = ReceiveMessageRequest.builder().build();

    SqsMessageReceiver messageHandler =
        new SqsMessageReceiver(getOpenTelemetry(), "destination", sqsClient);

    Span parentSpan1 = getOpenTelemetry().getTracer("test").spanBuilder("test1").startSpan();

    try (Scope scope = parentSpan1.makeCurrent()) {
      ReceiveMessageResponse response = messageHandler.receive(request);

      assertThat(response.messages().size()).isEqualTo(1);
      assertThat(response.messages().get(0).body()).isEqualTo("Hello");
      assertThat(response.messages().get(0).attributesAsStrings().get("AWSTraceHeader"))
          .isEqualTo("Root=1-55555555-123456789012345678901234;Parent=1234567890123456;Sampled=0");
    }

    parentSpan1.end();

    Span parentSpan2 = getOpenTelemetry().getTracer("test").spanBuilder("test2").startSpan();

    try (Scope scope = parentSpan2.makeCurrent()) {
      ReceiveMessageResponse response = messageHandler.receive(request);

      assertThat(response.messages().size()).isEqualTo(1);
      assertThat(response.messages().get(0).body()).isEqualTo("SecondMessage");
      assertThat(response.messages().get(0).attributesAsStrings().get("AWSTraceHeader"))
          .isEqualTo("Root=1-77555555-123456789012345678901234;Parent=7734567890123456;Sampled=1");
    }

    parentSpan2.end();

    waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("test1").hasTotalAttributeCount(0).hasTotalRecordedLinks(0),
                span ->
                    span.hasName("destination receive")
                        .hasKind(SpanKind.CONSUMER)
                        .hasLinks(
                            LinkData.create(
                                SpanContext.createFromRemoteParent(
                                    "55555555123456789012345678901234",
                                    "1234567890123456",
                                    TraceFlags.getDefault(),
                                    TraceState.getDefault())))
                        .hasTotalRecordedLinks(1)
                        .hasAttribute(SemanticAttributes.MESSAGING_OPERATION, "receive")
                        .hasAttribute(SemanticAttributes.MESSAGING_SYSTEM, "AmazonSQS")
                        .hasAttribute(SemanticAttributes.MESSAGING_DESTINATION_NAME, "destination")
                        .hasAttribute(SemanticAttributes.MESSAGING_MESSAGE_PAYLOAD_SIZE_BYTES, 5L)
                        .hasTotalAttributeCount(4)
                        .hasParentSpanId(parentSpan1.getSpanContext().getSpanId())
                        .hasTraceId(parentSpan1.getSpanContext().getTraceId())),
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("test2").hasTotalAttributeCount(0).hasTotalRecordedLinks(0),
                span ->
                    span.hasName("destination receive")
                        .hasKind(SpanKind.CONSUMER)
                        .hasLinks(
                            LinkData.create(
                                SpanContext.createFromRemoteParent(
                                    "77555555123456789012345678901234",
                                    "7734567890123456",
                                    TraceFlags.getSampled(),
                                    TraceState.getDefault())))
                        .hasTotalRecordedLinks(1)
                        .hasAttribute(SemanticAttributes.MESSAGING_OPERATION, "receive")
                        .hasAttribute(SemanticAttributes.MESSAGING_SYSTEM, "AmazonSQS")
                        .hasAttribute(SemanticAttributes.MESSAGING_DESTINATION_NAME, "destination")
                        .hasAttribute(SemanticAttributes.MESSAGING_MESSAGE_PAYLOAD_SIZE_BYTES, 13L)
                        .hasTotalAttributeCount(4)
                        .hasParentSpanId(parentSpan2.getSpanContext().getSpanId())
                        .hasTraceId(parentSpan2.getSpanContext().getTraceId())));
  }

  @Test
  public void noMessages() {
    SqsClient sqsClient = getClient(ReceiveMessageResponse.builder().build());

    ReceiveMessageRequest request = ReceiveMessageRequest.builder().build();

    SqsMessageReceiver messageHandler =
        new SqsMessageReceiver(getOpenTelemetry(), "destination", sqsClient);

    Span parentSpan = getOpenTelemetry().getTracer("test").spanBuilder("test").startSpan();

    try (Scope scope = parentSpan.makeCurrent()) {
      ReceiveMessageResponse response = messageHandler.receive(request);

      assertThat(response.messages().size()).isEqualTo(0);
    }

    parentSpan.end();

    waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("test").hasTotalAttributeCount(0).hasTotalRecordedLinks(0),
                span ->
                    span.hasName("destination receive")
                        .hasKind(SpanKind.CONSUMER)
                        .hasTotalRecordedLinks(0)
                        .hasAttribute(SemanticAttributes.MESSAGING_OPERATION, "receive")
                        .hasAttribute(SemanticAttributes.MESSAGING_SYSTEM, "AmazonSQS")
                        .hasAttribute(SemanticAttributes.MESSAGING_DESTINATION_NAME, "destination")
                        .hasAttribute(SemanticAttributes.MESSAGING_MESSAGE_PAYLOAD_SIZE_BYTES, 0L)
                        .hasTotalAttributeCount(4)
                        .hasParentSpanId(parentSpan.getSpanContext().getSpanId())
                        .hasTraceId(parentSpan.getSpanContext().getTraceId())));
  }

  @Test
  public void malformedTraceId() {
    SqsClient sqsClient =
        getClient(
            ReceiveMessageResponse.builder()
                .messages(
                    Message.builder()
                        .body("Hello")
                        .attributesWithStrings(
                            Collections.singletonMap(
                                "AWSTraceHeader",
                                "Root=1-55555555-error;Parent=1234567890123456;Sampled=1"))
                        .build())
                .build());

    ReceiveMessageRequest request = ReceiveMessageRequest.builder().build();

    SqsMessageReceiver messageHandler =
        new SqsMessageReceiver(getOpenTelemetry(), "destination", sqsClient);

    Span parentSpan = getOpenTelemetry().getTracer("test").spanBuilder("test").startSpan();

    assertThrows(
        RuntimeException.class,
        () -> {
          try (Scope scope = parentSpan.makeCurrent()) {
            messageHandler.receive(request);
          }
        });

    parentSpan.end();

    waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("test").hasTotalAttributeCount(0).hasTotalRecordedLinks(0)));
  }

  private SqsClient getClient(ReceiveMessageResponse... responses) {
    return new SqsClient() {
      int messageAt = 0;

      @Override
      public String serviceName() {
        return "MyService";
      }

      @Override
      public ReceiveMessageResponse receiveMessage(ReceiveMessageRequest request) {
        return responses[messageAt++];
      }

      @Override
      public void close() {}
    };
  }
}
