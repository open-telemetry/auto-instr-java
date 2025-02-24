/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.incubator.semconv.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.opentelemetry.instrumentation.api.internal.SemconvStability;
import io.opentelemetry.instrumentation.api.semconv.network.ServerAttributesGetter;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MessagingSpanNameExtractorTest {

  @Mock MessagingAttributesGetter<Message, Void> getter;
  @Mock ServerAttributesGetter<Message> serverAttributesGetter;

  @ParameterizedTest
  @MethodSource("spanNameParams")
  void shouldExtractSpanName(
      boolean isTemporaryQueue,
      boolean isAnonymous,
      String destinationName,
      String destinationTemplate,
      MessageOperation operation,
      String expectedSpanName) {
    // given
    Message message = new Message();

    when(getter.getDestinationTemplate(message)).thenReturn(destinationTemplate);
    if (isAnonymous) {
      lenient().when(getter.isAnonymousDestination(message)).thenReturn(true);
    }

    if (isTemporaryQueue) {
      lenient().when(getter.isTemporaryDestination(message)).thenReturn(true);
    } else {
      lenient().when(getter.getDestination(message)).thenReturn(destinationName);
    }
    when(getter.getOperationName(message)).thenReturn(operation.operationType());

    lenient().when(serverAttributesGetter.getServerPort(message)).thenReturn(1234);
    lenient().when(serverAttributesGetter.getServerAddress(message)).thenReturn("127.0.0.1");

    SpanNameExtractor<Message> underTest =
        MessagingSpanNameExtractor.create(getter, operation, serverAttributesGetter);

    // when
    String spanName = underTest.extract(message);

    // then
    assertEquals(expectedSpanName, spanName);
  }

  @SuppressWarnings("deprecation") // using deprecated semconv
  static Stream<Arguments> spanNameParams() {
    if (SemconvStability.emitStableMessagingSemconv()) {
      return Stream.of(
          Arguments.of(
              false,
              false,
              "destination/1",
              "destination/{}",
              MessageOperation.PUBLISH,
              "publish destination/{}"),
          Arguments.of(
              false,
              false,
              "destination/1",
              null,
              MessageOperation.PUBLISH,
              "publish destination/1"),
          Arguments.of(true, false, null, "temp", MessageOperation.PROCESS, "process temp"),
          Arguments.of(true, false, null, null, MessageOperation.PROCESS, "process (temporary)"),
          Arguments.of(false, true, null, "anon", MessageOperation.PROCESS, "process anon"),
          Arguments.of(false, true, null, null, MessageOperation.PROCESS, "process (anonymous)"),
          Arguments.of(true, true, null, null, MessageOperation.PROCESS, "process (temporary)"),
          Arguments.of(
              false, false, null, null, MessageOperation.RECEIVE, "receive 127.0.0.1:1234"));
    }
    return Stream.of(
        Arguments.of(
            false,
            false,
            "destination",
            "not used",
            MessageOperation.PUBLISH,
            "destination publish"),
        Arguments.of(
            true, false, null, "not used", MessageOperation.PROCESS, "(temporary) process"),
        Arguments.of(false, false, null, "not used", MessageOperation.RECEIVE, "unknown receive"));
  }

  static class Message {}
}
