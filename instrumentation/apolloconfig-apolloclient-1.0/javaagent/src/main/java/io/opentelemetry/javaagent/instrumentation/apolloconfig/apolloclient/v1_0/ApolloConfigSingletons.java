/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apolloconfig.apolloclient.v1_0;

import static io.opentelemetry.api.common.AttributeKey.stringKey;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor;
import javax.annotation.Nullable;

public final class ApolloConfigSingletons {

  private static final String INSTRUMENTATION_NAME =
      "io.opentelemetry.apolloconfig-apolloclient-1.0";
  private static final Instrumenter<String, Void> INSTRUMENTER;

  private static final AttributeKey<String> CONFIG_NS_ATTRIBUTE_KEY = stringKey("config.namespace");
  public static final ContextKey<String> REPOSITORY_CHANGE_REPEAT_CONTEXT_KEY =
      ContextKey.named("apollo-config-repository-change-repeat");

  static {
    AttributesExtractor<String, Void> attributesExtractor =
        new AttributesExtractor<String, Void>() {

          @Override
          public void onStart(
              AttributesBuilder attributes, Context parentContext, String namespace) {
            if (namespace == null) {
              return;
            }

            attributes.put(CONFIG_NS_ATTRIBUTE_KEY, namespace);
          }

          @Override
          public void onEnd(
              AttributesBuilder attributes,
              Context context,
              String namespace,
              @Nullable Void unused,
              @Nullable Throwable error) {}
        };

    INSTRUMENTER =
        Instrumenter.<String, Void>builder(
                GlobalOpenTelemetry.get(),
                INSTRUMENTATION_NAME,
                (event) -> "Apollo Config Repository Change")
            .setSpanStatusExtractor(
                (spanStatusBuilder, request, unused, error) -> {
                  if (error != null) {
                    spanStatusBuilder.setStatus(StatusCode.ERROR);
                  }
                })
            .addAttributesExtractor(attributesExtractor)
            .buildInstrumenter(SpanKindExtractor.alwaysInternal());
  }

  public static Instrumenter<String, Void> instrumenter() {
    return INSTRUMENTER;
  }

  private ApolloConfigSingletons() {}
}
