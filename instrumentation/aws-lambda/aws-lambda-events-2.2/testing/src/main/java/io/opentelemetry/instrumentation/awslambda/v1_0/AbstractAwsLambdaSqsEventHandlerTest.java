/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.awslambda.v1_0;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.when;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.SQSEvent;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.semconv.trace.attributes.SemanticAttributes;
import java.lang.reflect.Constructor;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public abstract class AbstractAwsLambdaSqsEventHandlerTest {

  private static final String AWS_TRACE_HEADER =
      "Root=1-5759e988-bd862e3fe1be46a994272793;Parent=53995c3f42cd8ad8;Sampled=1";

  protected abstract RequestHandler<SQSEvent, Void> handler();

  protected abstract InstrumentationExtension testing();

  @Mock private Context context;

  @BeforeEach
  void setUp() {
    when(context.getFunctionName()).thenReturn("my_function");
    when(context.getAwsRequestId()).thenReturn("1-22-333");
  }

  @AfterEach
  void tearDown() {
    assertThat(testing().forceFlushCalled()).isTrue();
  }

  @Test
  void sameSource() {
    SQSEvent.SQSMessage message1 = newMessage();
    message1.setAttributes(Collections.singletonMap("AWSTraceHeader", AWS_TRACE_HEADER));
    message1.setMessageId("message1");
    message1.setEventSource("queue1");

    SQSEvent.SQSMessage message2 = newMessage();
    message2.setAttributes(Collections.emptyMap());
    message2.setMessageId("message2");
    message2.setEventSource("queue1");

    SQSEvent event = new SQSEvent();
    event.setRecords(Arrays.asList(message1, message2));

    handler().handleRequest(event, context);

    testing()
        .waitAndAssertTraces(
            trace ->
                trace.hasSpansSatisfyingExactly(
                    span ->
                        span.hasName("my_function")
                            .hasKind(SpanKind.SERVER)
                            .hasAttributesSatisfying(
                                attrs ->
                                    assertThat(attrs)
                                        .containsOnly(
                                            entry(SemanticAttributes.FAAS_EXECUTION, "1-22-333"))),
                    span ->
                        span.hasName("queue1 process")
                            .hasKind(SpanKind.CONSUMER)
                            .hasParentSpanId(trace.getSpan(0).getSpanId())
                            .hasAttributesSatisfying(
                                attrs ->
                                    assertThat(attrs)
                                        .containsOnly(
                                            entry(SemanticAttributes.MESSAGING_SYSTEM, "AmazonSQS"),
                                            entry(
                                                SemanticAttributes.MESSAGING_OPERATION, "process")))
                            .hasLinksSatisfying(
                                links ->
                                    assertThat(links)
                                        .singleElement()
                                        .satisfies(
                                            link -> {
                                              assertThat(link.getSpanContext().getTraceId())
                                                  .isEqualTo("5759e988bd862e3fe1be46a994272793");
                                              assertThat(link.getSpanContext().getSpanId())
                                                  .isEqualTo("53995c3f42cd8ad8");
                                            }))));
  }

  @Test
  void differentSource() {
    SQSEvent.SQSMessage message1 = newMessage();
    message1.setAttributes(Collections.singletonMap("AWSTraceHeader", AWS_TRACE_HEADER));
    message1.setMessageId("message1");
    message1.setEventSource("queue1");

    SQSEvent.SQSMessage message2 = newMessage();
    message2.setAttributes(Collections.emptyMap());
    message2.setMessageId("message2");
    message2.setEventSource("queue2");

    SQSEvent event = new SQSEvent();
    event.setRecords(Arrays.asList(message1, message2));

    handler().handleRequest(event, context);

    testing()
        .waitAndAssertTraces(
            trace ->
                trace.hasSpansSatisfyingExactly(
                    span ->
                        span.hasName("my_function")
                            .hasKind(SpanKind.SERVER)
                            .hasAttributesSatisfying(
                                attrs ->
                                    assertThat(attrs)
                                        .containsOnly(
                                            entry(SemanticAttributes.FAAS_EXECUTION, "1-22-333"))),
                    span ->
                        span.hasName("multiple_sources process")
                            .hasKind(SpanKind.CONSUMER)
                            .hasParentSpanId(trace.getSpan(0).getSpanId())
                            .hasAttributesSatisfying(
                                attrs ->
                                    assertThat(attrs)
                                        .containsOnly(
                                            entry(SemanticAttributes.MESSAGING_SYSTEM, "AmazonSQS"),
                                            entry(
                                                SemanticAttributes.MESSAGING_OPERATION, "process")))
                            .hasLinksSatisfying(
                                links ->
                                    assertThat(links)
                                        .singleElement()
                                        .satisfies(
                                            link -> {
                                              assertThat(link.getSpanContext().getTraceId())
                                                  .isEqualTo("5759e988bd862e3fe1be46a994272793");
                                              assertThat(link.getSpanContext().getSpanId())
                                                  .isEqualTo("53995c3f42cd8ad8");
                                            }))));
  }

  // Constructor private in early versions.
  private static SQSEvent.SQSMessage newMessage() {
    try {
      Constructor<SQSEvent.SQSMessage> ctor = SQSEvent.SQSMessage.class.getDeclaredConstructor();
      ctor.setAccessible(true);
      return ctor.newInstance();
    } catch (Throwable t) {
      throw new AssertionError(t);
    }
  }
}
