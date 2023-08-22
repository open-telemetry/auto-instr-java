/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awssdk.v2_2;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor;
import io.opentelemetry.sdk.trace.data.LinkData;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.MessageAttributeValue;

public class SqsMessageHandlerTest extends XrayTestInstrumenter {
  private static class SqsMessageHandlerImpl extends SqsMessageHandler {
    public final AtomicInteger handleCalls = new AtomicInteger();

    public SqsMessageHandlerImpl(OpenTelemetry openTelemetry,
        String destination) {
      super(openTelemetry, destination);
    }

    @Override
    protected void doHandle(Collection<Message> request) {
      handleCalls.getAndIncrement();
    }
  }

  @Test
  public void simple() {
    SqsMessageHandlerImpl messageHandler = new SqsMessageHandlerImpl(
        getOpenTelemetry(),
        "destination");

    messageHandler.setSpanNameExtactor(e-> "MySpan");

    List<Message> messages = new LinkedList<>();
    messages.add(Message.builder()
        .body("Hello")
        .attributesWithStrings(
            Collections.singletonMap(
                "AWSTraceHeader",
                "Root=1-55555555-123456789012345678901234;Parent=1234567890123456;Sampled=1"))
        .build());

    Span parentSpan = getOpenTelemetry().getTracer("test").spanBuilder("test").startSpan();

    try (Scope scope = parentSpan.makeCurrent()) {
      messageHandler.handle(messages);
    }

    parentSpan.end();

    assertThat(messageHandler.handleCalls.get()).isEqualTo(1);

    waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("test").hasTotalAttributeCount(0).hasTotalRecordedLinks(0),
                span ->
                    span.hasName("MySpan")
                        .hasKind(SpanKind.CONSUMER)
                        .hasLinks(
                            LinkData.create(
                                SpanContext.createFromRemoteParent(
                                    "55555555123456789012345678901234",
                                    "1234567890123456",
                                    TraceFlags.getSampled(),
                                    TraceState.getDefault())))
                        .hasTotalRecordedLinks(1)
                        .hasAttribute(SemanticAttributes.MESSAGING_OPERATION, "process")
                        .hasAttribute(SemanticAttributes.MESSAGING_SYSTEM, "AmazonSQS")
                        .hasAttribute(SemanticAttributes.MESSAGING_DESTINATION_NAME, "destination")
                        .hasAttribute(SemanticAttributes.MESSAGING_MESSAGE_PAYLOAD_SIZE_BYTES, 5L)
                        .hasTotalAttributeCount(4)
                        .hasParentSpanId(parentSpan.getSpanContext().getSpanId())
                        .hasTraceId(parentSpan.getSpanContext().getTraceId())));
  }

  @Test
  public void simpleUnsampled() {
    SqsMessageHandlerImpl messageHandler = new SqsMessageHandlerImpl(
        getOpenTelemetry(),
        "destination");

    messageHandler.setSpanNameExtactor(e-> "MySpan");

    List<Message> messages = new LinkedList<>();
    messages.add(Message.builder()
        .body("Hello")
        .attributesWithStrings(
            Collections.singletonMap(
                "AWSTraceHeader",
                "Root=1-55555555-123456789012345678901234;Parent=1234567890123456;Sampled=0"))
        .build());

    Span parentSpan = getOpenTelemetry().getTracer("test").spanBuilder("test").startSpan();

    try (Scope scope = parentSpan.makeCurrent()) {
      messageHandler.handle(messages);
    }

    parentSpan.end();

    assertThat(messageHandler.handleCalls.get()).isEqualTo(1);

    waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("test").hasTotalAttributeCount(0).hasTotalRecordedLinks(0),
                span ->
                    span.hasName("MySpan")
                        .hasKind(SpanKind.CONSUMER)
                        .hasLinks(
                            LinkData.create(
                                SpanContext.createFromRemoteParent(
                                    "55555555123456789012345678901234",
                                    "1234567890123456",
                                    TraceFlags.getDefault(),
                                    TraceState.getDefault())))
                        .hasAttribute(SemanticAttributes.MESSAGING_OPERATION, "process")
                        .hasAttribute(SemanticAttributes.MESSAGING_SYSTEM, "AmazonSQS")
                        .hasAttribute(SemanticAttributes.MESSAGING_DESTINATION_NAME, "destination")
                        .hasAttribute(SemanticAttributes.MESSAGING_MESSAGE_PAYLOAD_SIZE_BYTES, 5L)
                        .hasTotalAttributeCount(4)
                        .hasParentSpanId(parentSpan.getSpanContext().getSpanId())
                        .hasTraceId(parentSpan.getSpanContext().getTraceId())));
  }

  @Test
  public void simpleUseMessageAttribute() {
    SqsMessageHandlerImpl messageHandler = new SqsMessageHandlerImpl(
        getOpenTelemetry(),
        "destination");

    List<Message> messages = new LinkedList<>();
    messages.add(Message.builder()
        .body("Hello")
            .messageAttributes(Collections.singletonMap("X-Amzn-Trace-Id",
                MessageAttributeValue.builder().dataType("String")
                    .stringValue("Root=1-55555555-123456789012345678901234;Parent=1234567890123456;Sampled=1")
                    .build()))
        .build());

    Span parentSpan = getOpenTelemetry()
        .getTracer("test")
        .spanBuilder("test")
        .startSpan();

    try (Scope scope = parentSpan.makeCurrent()) {
      messageHandler.handle(messages);
    }

    parentSpan.end();

    assertThat(messageHandler.handleCalls.get()).isEqualTo(1);

    waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("test").hasTotalAttributeCount(0).hasTotalRecordedLinks(0),
                span ->
                    span.hasName("destination process")
                        .hasKind(SpanKind.CONSUMER)
                        .hasLinks(
                            LinkData.create(
                                SpanContext.createFromRemoteParent(
                                    "55555555123456789012345678901234",
                                    "1234567890123456",
                                    TraceFlags.getSampled(),
                                    TraceState.getDefault())))
                        .hasTotalRecordedLinks(1)
                        .hasAttribute(SemanticAttributes.MESSAGING_OPERATION, "process")
                        .hasAttribute(SemanticAttributes.MESSAGING_SYSTEM, "AmazonSQS")
                        .hasAttribute(SemanticAttributes.MESSAGING_DESTINATION_NAME, "destination")
                        .hasAttribute(SemanticAttributes.MESSAGING_MESSAGE_PAYLOAD_SIZE_BYTES, 5L)
                        .hasTotalAttributeCount(4)
                        .hasParentSpanId(parentSpan.getSpanContext().getSpanId())
                        .hasTraceId(parentSpan.getSpanContext().getTraceId())));
  }

  @Test
  public void twoMessages() {
    List<Message> messages = new LinkedList<Message>();
    messages.add(Message.builder()
        .body("Hello")
        .attributesWithStrings(
            Collections.singletonMap(
                "AWSTraceHeader",
                "Root=1-55555555-123456789012345678901234;Parent=1234567890123456;Sampled=1"))
        .build());
    messages.add(Message.builder()
        .body("Hello World")
        .attributesWithStrings(
            Collections.singletonMap(
                "AWSTraceHeader",
                "Root=1-66555555-123456789012345678901234;Parent=6634567890123456;Sampled=1"))
        .build());

    SqsMessageHandlerImpl messageHandler = new SqsMessageHandlerImpl(
        getOpenTelemetry(),
        "destination");

    messageHandler.setSpanKindExtractor(SpanKindExtractor.alwaysConsumer());

    Span parentSpan = getOpenTelemetry().getTracer("test").spanBuilder("test").startSpan();

    try (Scope scope = parentSpan.makeCurrent()) {
      messageHandler.handle(messages);
    }

    parentSpan.end();

    assertThat(messageHandler.handleCalls.get()).isEqualTo(1);

    waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("test").hasTotalAttributeCount(0).hasTotalRecordedLinks(0),
                span ->
                    span.hasName("destination process")
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
                                    TraceFlags.getSampled(),
                                    TraceState.getDefault())))
                        .hasTotalRecordedLinks(2)
                        .hasAttribute(SemanticAttributes.MESSAGING_OPERATION, "process")
                        .hasAttribute(SemanticAttributes.MESSAGING_SYSTEM, "AmazonSQS")
                        .hasAttribute(SemanticAttributes.MESSAGING_DESTINATION_NAME, "destination")
                        .hasAttribute(SemanticAttributes.MESSAGING_MESSAGE_PAYLOAD_SIZE_BYTES, 16L)
                        .hasTotalAttributeCount(4)
                        .hasParentSpanId(parentSpan.getSpanContext().getSpanId())
                        .hasTraceId(parentSpan.getSpanContext().getTraceId())));
  }

  @Test
  public void twoRuns() {
    List<Message> messages1 = new LinkedList<Message>();
    messages1.add(Message.builder()
      .body("Hello")
      .attributesWithStrings(
          Collections.singletonMap(
              "AWSTraceHeader",
              "Root=1-55555555-123456789012345678901234;Parent=1234567890123456;Sampled=1"))
      .build());

    List<Message> messages2 = new LinkedList<Message>();
    messages2.add(Message.builder()
      .body("SecondMessage")
      .attributesWithStrings(
          Collections.singletonMap(
              "AWSTraceHeader",
              "Root=1-77555555-123456789012345678901234;Parent=7734567890123456;Sampled=1"))
      .build());

    SqsMessageHandlerImpl messageHandler = new SqsMessageHandlerImpl(
        getOpenTelemetry(),
        "destination");

    Span parentSpan1 = getOpenTelemetry().getTracer("test").spanBuilder("test1").startSpan();
    try (Scope scope = parentSpan1.makeCurrent()) {
      messageHandler.handle(messages1);
    }
    parentSpan1.end();

    Span parentSpan2 = getOpenTelemetry().getTracer("test").spanBuilder("test2").startSpan();
    try (Scope scope = parentSpan2.makeCurrent()) {
      messageHandler.handle(messages2);
    }
    parentSpan2.end();

    assertThat(messageHandler.handleCalls.get()).isEqualTo(2);

    waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("test1").hasTotalAttributeCount(0).hasTotalRecordedLinks(0),
                span ->
                    span.hasName("destination process")
                        .hasKind(SpanKind.CONSUMER)
                        .hasLinks(
                            LinkData.create(
                                SpanContext.createFromRemoteParent(
                                    "55555555123456789012345678901234",
                                    "1234567890123456",
                                    TraceFlags.getSampled(),
                                    TraceState.getDefault())))
                        .hasTotalRecordedLinks(1)
                        .hasAttribute(SemanticAttributes.MESSAGING_OPERATION, "process")
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
                    span.hasName("destination process")
                        .hasKind(SpanKind.CONSUMER)
                        .hasLinks(
                            LinkData.create(
                                SpanContext.createFromRemoteParent(
                                    "77555555123456789012345678901234",
                                    "7734567890123456",
                                    TraceFlags.getSampled(),
                                    TraceState.getDefault())))
                        .hasTotalRecordedLinks(1)
                        .hasAttribute(SemanticAttributes.MESSAGING_OPERATION, "process")
                        .hasAttribute(SemanticAttributes.MESSAGING_SYSTEM, "AmazonSQS")
                        .hasAttribute(SemanticAttributes.MESSAGING_DESTINATION_NAME, "destination")
                        .hasAttribute(SemanticAttributes.MESSAGING_MESSAGE_PAYLOAD_SIZE_BYTES, 13L)
                        .hasTotalAttributeCount(4)
                        .hasParentSpanId(parentSpan2.getSpanContext().getSpanId())
                        .hasTraceId(parentSpan2.getSpanContext().getTraceId())));
  }

  @Test
  public void noMessages() {
    SqsMessageHandlerImpl messageHandler = new SqsMessageHandlerImpl(
        getOpenTelemetry(),
        "destination");

    Span parentSpan = getOpenTelemetry().getTracer("test").spanBuilder("test").startSpan();
    try (Scope scope = parentSpan.makeCurrent()) {
      messageHandler.handle(new LinkedList<>());
    }
    parentSpan.end();

    assertThat(messageHandler.handleCalls.get()).isEqualTo(1);

    waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("test").hasTotalAttributeCount(0).hasTotalRecordedLinks(0),
                span ->
                    span.hasName("destination process")
                        .hasKind(SpanKind.CONSUMER)
                        .hasTotalRecordedLinks(0)
                        .hasAttribute(SemanticAttributes.MESSAGING_OPERATION, "process")
                        .hasAttribute(SemanticAttributes.MESSAGING_SYSTEM, "AmazonSQS")
                        .hasAttribute(SemanticAttributes.MESSAGING_DESTINATION_NAME, "destination")
                        .hasAttribute(SemanticAttributes.MESSAGING_MESSAGE_PAYLOAD_SIZE_BYTES, 0L)
                        .hasTotalAttributeCount(4)
                        .hasParentSpanId(parentSpan.getSpanContext().getSpanId())
                        .hasTraceId(parentSpan.getSpanContext().getTraceId())));
  }

  @Test
  public void malformedTraceId() {
    List<Message> messages = new LinkedList<Message>();
    messages.add(Message.builder()
      .body("Hello")
      .attributesWithStrings(
          Collections.singletonMap(
              "AWSTraceHeader",
              "Root=1-55555555-error;Parent=1234567890123456;Sampled=1"))
      .build());

    SqsMessageHandlerImpl messageHandler = new SqsMessageHandlerImpl(
        getOpenTelemetry(),
        "destination");

    Span parentSpan = getOpenTelemetry().getTracer("test").spanBuilder("test").startSpan();

    assertThrows(
        RuntimeException.class,
        () -> {
          try (Scope scope = parentSpan.makeCurrent()) {
            messageHandler.handle(messages);
          }
        });

    parentSpan.end();

    assertThat(messageHandler.handleCalls.get()).isEqualTo(0);

    waitAndAssertTraces(
        trace ->
            trace.hasSpansSatisfyingExactly(
                span -> span.hasName("test").hasTotalAttributeCount(0).hasTotalRecordedLinks(0)));
  }
}
