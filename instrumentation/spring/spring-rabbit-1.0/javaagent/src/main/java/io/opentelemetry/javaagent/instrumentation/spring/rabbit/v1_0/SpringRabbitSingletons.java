/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.spring.rabbit.v1_0;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessageOperation;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingAttributesExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingNetworkAttributesExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.messaging.MessagingSpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.javaagent.bootstrap.internal.ExperimentalConfig;
import org.springframework.amqp.core.Message;

public final class SpringRabbitSingletons {

  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.spring-rabbit-1.0";

  private static final Instrumenter<Message, Void> INSTRUMENTER;

  static {
    SpringRabbitMessageAttributesGetter getter = SpringRabbitMessageAttributesGetter.INSTANCE;
    MessageOperation operation = MessageOperation.PROCESS;

    INSTRUMENTER =
        Instrumenter.<Message, Void>builder(
                GlobalOpenTelemetry.get(),
                INSTRUMENTATION_NAME,
                MessagingSpanNameExtractor.create(getter, operation, getter))
            .addAttributesExtractor(
                MessagingAttributesExtractor.builder(getter, operation)
                    .setCapturedHeaders(ExperimentalConfig.get().getMessagingHeaders())
                    .build())
            .addAttributesExtractor(MessagingNetworkAttributesExtractor.create(getter))
            .buildConsumerInstrumenter(MessageHeaderGetter.INSTANCE);
  }

  public static Instrumenter<Message, Void> instrumenter() {
    return INSTRUMENTER;
  }

  private SpringRabbitSingletons() {}
}
