/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.incubator.semconv.messaging;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.internal.SemconvStability;
import io.opentelemetry.semconv.incubating.MessagingIncubatingAttributes;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import javax.annotation.Nullable;
import org.assertj.core.data.MapEntry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class MessagingAttributesExtractorTest {

  @SuppressWarnings("deprecation") // using deprecated semconv
  @ParameterizedTest
  @MethodSource("destinations")
  void shouldExtractAllAvailableAttributes(
      boolean temporary,
      boolean anonymous,
      String destination,
      MessageOperation operation,
      String expectedDestination) {
    // given
    Map<String, String> request = new HashMap<>();
    request.put("system", "myQueue");
    request.put("destinationKind", "topic");
    request.put("destination", destination);
    request.put("destinationTemplate", destination + "-template");
    if (temporary) {
      request.put("temporaryDestination", "y");
    }
    if (anonymous) {
      request.put("anonymousDestination", "y");
    }
    request.put("url", "http://broker/topic");
    request.put("conversationId", "42");
    request.put("bodySize", "100");
    request.put("envelopeSize", "120");
    request.put("clientId", "43");
    request.put("batchMessageCount", "2");
    request.put("operationName", "ack");

    AttributesExtractor<Map<String, String>, String> underTest =
        MessagingAttributesExtractor.create(TestGetter.INSTANCE, operation);

    Context context = Context.root();

    // when
    AttributesBuilder startAttributes = Attributes.builder();
    underTest.onStart(startAttributes, context, request);

    AttributesBuilder endAttributes = Attributes.builder();
    underTest.onEnd(endAttributes, context, request, "42", null);

    // then
    List<MapEntry<AttributeKey<?>, Object>> expectedEntries = new ArrayList<>();
    expectedEntries.add(entry(MessagingIncubatingAttributes.MESSAGING_SYSTEM, "myQueue"));
    expectedEntries.add(
        entry(MessagingIncubatingAttributes.MESSAGING_DESTINATION_NAME, expectedDestination));
    if (temporary) {
      expectedEntries.add(
          entry(MessagingIncubatingAttributes.MESSAGING_DESTINATION_TEMPORARY, true));
    } else {
      expectedEntries.add(
          entry(
              MessagingIncubatingAttributes.MESSAGING_DESTINATION_TEMPLATE,
              destination + "-template"));
    }
    if (anonymous) {
      expectedEntries.add(
          entry(MessagingIncubatingAttributes.MESSAGING_DESTINATION_ANONYMOUS, true));
    }

    if (SemconvStability.emitOldMessagingSemconv()) {
      expectedEntries.add(entry(AttributeKey.stringKey("messaging.client_id"), "43"));
      expectedEntries.add(
          entry(MessagingIncubatingAttributes.MESSAGING_OPERATION, operation.operationName()));
    }
    if (SemconvStability.emitStableMessagingSemconv()) {
      expectedEntries.add(entry(MessagingIncubatingAttributes.MESSAGING_CLIENT_ID, "43"));
      expectedEntries.add(
          entry(MessagingIncubatingAttributes.MESSAGING_OPERATION_TYPE, operation.operationType()));
      expectedEntries.add(entry(MessagingIncubatingAttributes.MESSAGING_OPERATION_NAME, "ack"));
    }

    expectedEntries.add(
        entry(MessagingIncubatingAttributes.MESSAGING_MESSAGE_CONVERSATION_ID, "42"));

    @SuppressWarnings({"unchecked", "rawtypes"})
    MapEntry<? extends AttributeKey<?>, ?>[] expectedEntriesArr =
        expectedEntries.toArray(new MapEntry[0]);
    assertThat(startAttributes.build()).containsOnly(expectedEntriesArr);

    assertThat(endAttributes.build())
        .containsOnly(
            entry(MessagingIncubatingAttributes.MESSAGING_MESSAGE_ID, "42"),
            entry(MessagingIncubatingAttributes.MESSAGING_BATCH_MESSAGE_COUNT, 2L),
            entry(MessagingIncubatingAttributes.MESSAGING_MESSAGE_BODY_SIZE, 100L),
            entry(MessagingIncubatingAttributes.MESSAGING_MESSAGE_ENVELOPE_SIZE, 120L));
  }

  static Stream<Arguments> destinations() {
    return Stream.of(
        Arguments.of(false, false, "destination", MessageOperation.RECEIVE, "destination"),
        Arguments.of(true, false, "destination", MessageOperation.PROCESS, "(temporary)"),
        Arguments.of(false, true, "destination", MessageOperation.PROCESS, "(anonymous)"),
        Arguments.of(true, true, "destination", MessageOperation.PROCESS, "(temporary)"));
  }

  @Test
  @SuppressWarnings("deprecation") // using deprecated semconv
  void shouldExtractNoAttributesIfNoneAreAvailable() {
    // given
    AttributesExtractor<Map<String, String>, String> underTest =
        MessagingAttributesExtractor.create(TestGetter.INSTANCE, MessageOperation.CREATE);

    Context context = Context.root();

    // when
    AttributesBuilder startAttributes = Attributes.builder();
    underTest.onStart(startAttributes, context, Collections.emptyMap());

    AttributesBuilder endAttributes = Attributes.builder();
    underTest.onEnd(endAttributes, context, Collections.emptyMap(), null, null);

    // then
    List<MapEntry<AttributeKey<?>, Object>> expectedEntries = new ArrayList<>();
    if (SemconvStability.emitOldMessagingSemconv()) {
      expectedEntries.add(
          entry(
              MessagingIncubatingAttributes.MESSAGING_OPERATION,
              MessageOperation.CREATE.operationName()));
    }
    if (SemconvStability.emitStableMessagingSemconv()) {
      expectedEntries.add(
          entry(
              MessagingIncubatingAttributes.MESSAGING_OPERATION_TYPE,
              MessageOperation.CREATE.operationType()));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    MapEntry<? extends AttributeKey<?>, ?>[] expectedEntriesArr =
        expectedEntries.toArray(new MapEntry[0]);

    assertThat(startAttributes.build()).containsOnly(expectedEntriesArr);
    assertThat(endAttributes.build().isEmpty()).isTrue();
  }

  enum TestGetter implements MessagingAttributesGetter<Map<String, String>, String> {
    INSTANCE;

    @Override
    public String getSystem(Map<String, String> request) {
      return request.get("system");
    }

    @Override
    public String getDestination(Map<String, String> request) {
      return request.get("destination");
    }

    @Nullable
    @Override
    public String getDestinationTemplate(Map<String, String> request) {
      return request.get("destinationTemplate");
    }

    @Override
    public boolean isTemporaryDestination(Map<String, String> request) {
      return request.containsKey("temporaryDestination");
    }

    @Override
    public boolean isAnonymousDestination(Map<String, String> request) {
      return request.containsKey("anonymousDestination");
    }

    @Override
    public String getConversationId(Map<String, String> request) {
      return request.get("conversationId");
    }

    @Nullable
    @Override
    public Long getMessageBodySize(Map<String, String> request) {
      String payloadSize = request.get("bodySize");
      return payloadSize == null ? null : Long.valueOf(payloadSize);
    }

    @Nullable
    @Override
    public Long getMessageEnvelopeSize(Map<String, String> request) {
      String payloadSize = request.get("envelopeSize");
      return payloadSize == null ? null : Long.valueOf(payloadSize);
    }

    @Override
    public String getMessageId(Map<String, String> request, String response) {
      return response;
    }

    @Nullable
    @Override
    public String getClientId(Map<String, String> request) {
      return request.get("clientId");
    }

    @Nullable
    @Override
    public Long getBatchMessageCount(Map<String, String> request, @Nullable String response) {
      String payloadSize = request.get("batchMessageCount");
      return payloadSize == null ? null : Long.valueOf(payloadSize);
    }

    @Nullable
    @Override
    public String getOperationName(Map<String, String> request) {
      return request.get("operationName");
    }
  }
}
