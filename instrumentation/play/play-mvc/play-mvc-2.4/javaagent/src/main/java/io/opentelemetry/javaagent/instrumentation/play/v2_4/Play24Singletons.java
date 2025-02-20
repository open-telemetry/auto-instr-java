/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.play.v2_4;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.incubator.semconv.code.CodeAttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.semconv.http.HttpServerRoute;
import io.opentelemetry.instrumentation.api.semconv.http.HttpServerRouteSource;
import io.opentelemetry.javaagent.bootstrap.internal.ExperimentalConfig;
import play.api.mvc.Request;
import scala.Option;

public final class Play24Singletons {
  private static final String SPAN_NAME = "play.request";
  private static final Instrumenter<ActionData, Void> INSTRUMENTER;

  static {
    INSTRUMENTER =
        Instrumenter.<ActionData, Void>builder(
                GlobalOpenTelemetry.get(), "io.opentelemetry.play-mvc-2.4", s -> SPAN_NAME)
            .addAttributesExtractor(
                CodeAttributesExtractor.create(new ActionCodeAttributesGetter()))
            .setEnabled(ExperimentalConfig.get().controllerTelemetryEnabled())
            .buildInstrumenter();
  }

  public static Instrumenter<ActionData, Void> instrumenter() {
    return INSTRUMENTER;
  }

  public static void updateSpan(Context context, Request<?> request) {
    String route = getRoute(request);
    if (route == null) {
      return;
    }

    Span.fromContext(context).updateName(route);
    HttpServerRoute.update(context, HttpServerRouteSource.CONTROLLER, route);
  }

  private static String getRoute(Request<?> request) {
    if (request != null) {
      Option<String> pathOption = request.tags().get("ROUTE_PATTERN");
      if (!pathOption.isEmpty()) {
        return pathOption.get();
      }
    }
    return null;
  }

  private Play24Singletons() {}
}
