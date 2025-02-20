/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.jms;

import static io.opentelemetry.instrumentation.api.internal.SemconvStability.emitStableMessagingSemconv;
import static java.util.Collections.emptyList;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessageOperation;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingNetworkAttributesExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingSpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor;
import io.opentelemetry.instrumentation.api.internal.PropagatorBasedSpanLinksExtractor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class JmsInstrumenterFactory {

  private final OpenTelemetry openTelemetry;
  private final String instrumentationName;
  private List<String> capturedHeaders = emptyList();
  private boolean messagingReceiveInstrumentationEnabled = false;

  public JmsInstrumenterFactory(OpenTelemetry openTelemetry, String instrumentationName) {
    this.openTelemetry = openTelemetry;
    this.instrumentationName = instrumentationName;
  }

  @CanIgnoreReturnValue
  public JmsInstrumenterFactory setCapturedHeaders(Collection<String> capturedHeaders) {
    this.capturedHeaders = new ArrayList<>(capturedHeaders);
    return this;
  }

  @CanIgnoreReturnValue
  public JmsInstrumenterFactory setMessagingReceiveInstrumentationEnabled(
      boolean messagingReceiveInstrumentationEnabled) {
    this.messagingReceiveInstrumentationEnabled = messagingReceiveInstrumentationEnabled;
    return this;
  }

  @SuppressWarnings("deprecation") // using deprecated semconv
  public Instrumenter<MessageWithDestination, Void> createProducerInstrumenter() {
    JmsMessageAttributesGetter getter = JmsMessageAttributesGetter.INSTANCE;
    MessageOperation operation =
        emitStableMessagingSemconv() ? MessageOperation.SEND : MessageOperation.PUBLISH;

    return Instrumenter.<MessageWithDestination, Void>builder(
            openTelemetry,
            instrumentationName,
            MessagingSpanNameExtractor.create(getter, operation, getter))
        .addAttributesExtractor(createMessagingAttributesExtractor(operation))
        .addAttributesExtractor(MessagingNetworkAttributesExtractor.create(getter))
        .buildProducerInstrumenter(MessagePropertySetter.INSTANCE);
  }

  public Instrumenter<MessageWithDestination, Void> createConsumerReceiveInstrumenter() {
    JmsMessageAttributesGetter getter = JmsMessageAttributesGetter.INSTANCE;
    MessageOperation operation = MessageOperation.RECEIVE;

    InstrumenterBuilder<MessageWithDestination, Void> builder =
        Instrumenter.<MessageWithDestination, Void>builder(
                openTelemetry,
                instrumentationName,
                MessagingSpanNameExtractor.create(getter, operation, getter))
            .addAttributesExtractor(createMessagingAttributesExtractor(operation))
            .addAttributesExtractor(MessagingNetworkAttributesExtractor.create(getter));
    if (messagingReceiveInstrumentationEnabled || emitStableMessagingSemconv()) {
      builder.addSpanLinksExtractor(
          new PropagatorBasedSpanLinksExtractor<>(
              openTelemetry.getPropagators().getTextMapPropagator(),
              MessagePropertyGetter.INSTANCE));
    }
    return builder.buildInstrumenter(SpanKindExtractor.alwaysConsumer());
  }

  public Instrumenter<MessageWithDestination, Void> createConsumerProcessInstrumenter(
      boolean canHaveReceiveInstrumentation) {
    JmsMessageAttributesGetter getter = JmsMessageAttributesGetter.INSTANCE;
    MessageOperation operation = MessageOperation.PROCESS;

    InstrumenterBuilder<MessageWithDestination, Void> builder =
        Instrumenter.<MessageWithDestination, Void>builder(
                openTelemetry,
                instrumentationName,
                MessagingSpanNameExtractor.create(getter, operation, getter))
            .addAttributesExtractor(createMessagingAttributesExtractor(operation))
            .addAttributesExtractor(MessagingNetworkAttributesExtractor.create(getter));
    if (canHaveReceiveInstrumentation
        && (messagingReceiveInstrumentationEnabled || emitStableMessagingSemconv())) {
      builder.addSpanLinksExtractor(
          new PropagatorBasedSpanLinksExtractor<>(
              openTelemetry.getPropagators().getTextMapPropagator(),
              MessagePropertyGetter.INSTANCE));
      return builder.buildInstrumenter(SpanKindExtractor.alwaysConsumer());
    } else {
      return builder.buildConsumerInstrumenter(MessagePropertyGetter.INSTANCE);
    }
  }

  private AttributesExtractor<MessageWithDestination, Void> createMessagingAttributesExtractor(
      MessageOperation operation) {
    return MessagingAttributesExtractor.builder(JmsMessageAttributesGetter.INSTANCE, operation)
        .setCapturedHeaders(capturedHeaders)
        .build();
  }
}
