/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.spring.pulsar.v1_0;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessageOperation;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingSpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.javaagent.bootstrap.internal.ExperimentalConfig;
import org.apache.pulsar.client.api.Message;

public final class SpringPulsarSingletons {
  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.spring-pulsar-1.0";
  private static final Instrumenter<Message<?>, Void> INSTRUMENTER;

  static {
    SpringPulsarMessageAttributesGetter getter = SpringPulsarMessageAttributesGetter.INSTANCE;
    MessageOperation operation = MessageOperation.PROCESS;

    INSTRUMENTER =
        Instrumenter.<Message<?>, Void>builder(
                GlobalOpenTelemetry.get(),
                INSTRUMENTATION_NAME,
                MessagingSpanNameExtractor.create(getter, operation))
            .addAttributesExtractor(
                MessagingAttributesExtractor.builder(getter, operation)
                    .setCapturedHeaders(ExperimentalConfig.get().getMessagingHeaders())
                    .build())
            .buildConsumerInstrumenter(MessageHeaderGetter.INSTANCE);
  }

  public static Instrumenter<Message<?>, Void> instrumenter() {
    return INSTRUMENTER;
  }

  private SpringPulsarSingletons() {}
}
