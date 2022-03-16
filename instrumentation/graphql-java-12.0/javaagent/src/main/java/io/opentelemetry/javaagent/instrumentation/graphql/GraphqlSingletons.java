/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.graphql;

import graphql.execution.instrumentation.ChainedInstrumentation;
import graphql.execution.instrumentation.Instrumentation;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.api.config.Config;
import io.opentelemetry.instrumentation.graphql.GraphQLTracing;
import io.opentelemetry.instrumentation.graphql.OpenTelemetryInstrumentation;
import java.util.ArrayList;
import java.util.List;

public final class GraphqlSingletons {

  private static final boolean QUERY_SANITIZATION_ENABLED =
      Config.get().getBoolean("otel.instrumentation.graphql.query-sanitizer.enabled", true);
  private static final boolean CAPTURE_EXPERIMENTAL_SPAN_ATTRIBUTES =
      Config.get().getBoolean("otel.instrumentation.graphql.experimental-span-attributes", false);

  private static final GraphQLTracing TRACING =
      GraphQLTracing.builder(GlobalOpenTelemetry.get())
          .setCaptureExperimentalSpanAttributes(CAPTURE_EXPERIMENTAL_SPAN_ATTRIBUTES)
          .setSanitizeQuery(QUERY_SANITIZATION_ENABLED)
          .build();

  private GraphqlSingletons() {}

  public static Instrumentation addInstrumentation(Instrumentation instrumentation) {
    Instrumentation ourInstrumentation = TRACING.newInstrumentation();
    if (instrumentation == null) {
      return ourInstrumentation;
    }
    if (instrumentation instanceof OpenTelemetryInstrumentation) {
      return instrumentation;
    }
    List<Instrumentation> instrumentationList = new ArrayList<>();
    if (instrumentation instanceof ChainedInstrumentation) {
      instrumentationList.addAll(((ChainedInstrumentation) instrumentation).getInstrumentations());
    } else {
      instrumentationList.add(instrumentation);
    }
    boolean containsOurInstrumentation =
        instrumentationList.stream().anyMatch(OpenTelemetryInstrumentation.class::isInstance);
    if (!containsOurInstrumentation) {
      instrumentationList.add(0, ourInstrumentation);
    }
    return new ChainedInstrumentation(instrumentationList);
  }
}
