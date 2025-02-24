/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.testing;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor;
import io.opentelemetry.instrumentation.api.internal.SpanKey;
import io.opentelemetry.instrumentation.api.semconv.http.HttpServerAttributesExtractor;
import io.opentelemetry.instrumentation.api.semconv.http.HttpServerRoute;
import io.opentelemetry.instrumentation.api.semconv.http.HttpServerRouteSource;

public final class AgentSpanTestingInstrumenter {

  private static final ContextKey<String> REQUEST_CONTEXT_KEY =
      ContextKey.named("test-request-key");

  private static final Instrumenter<String, Void> INSTRUMENTER =
      Instrumenter.<String, Void>builder(GlobalOpenTelemetry.get(), "test", request -> request)
          .addContextCustomizer(
              (context, request, startAttributes) -> context.with(REQUEST_CONTEXT_KEY, request))
          .buildInstrumenter(SpanKindExtractor.alwaysInternal());

  private static final Instrumenter<String, Void> HTTP_SERVER_INSTRUMENTER =
      Instrumenter.<String, Void>builder(GlobalOpenTelemetry.get(), "test", request -> request)
          .addAttributesExtractor(
              HttpServerAttributesExtractor.create(MockHttpServerAttributesGetter.INSTANCE))
          .addContextCustomizer(HttpServerRoute.create(MockHttpServerAttributesGetter.INSTANCE))
          .addContextCustomizer(
              (context, request, startAttributes) -> context.with(REQUEST_CONTEXT_KEY, request))
          .buildInstrumenter(SpanKindExtractor.alwaysServer());

  public static Context startHttpServerSpan(String name) {
    Context context = HTTP_SERVER_INSTRUMENTER.start(Context.current(), name);
    HttpServerRoute.update(context, HttpServerRouteSource.SERVER, "/test/server/*");
    return context;
  }

  public static void endHttpServer(Context context, Throwable error) {
    HTTP_SERVER_INSTRUMENTER.end(context, context.get(REQUEST_CONTEXT_KEY), null, error);
  }

  public static Context startSpanWithAllKeys(String name) {
    Context context = INSTRUMENTER.start(Context.current(), name);
    Span span = Span.fromContext(context);
    for (SpanKey spanKey : getAllSpanKeys()) {
      context = spanKey.storeInContext(context, span);
    }
    return context;
  }

  public static void end(Context context, Throwable error) {
    INSTRUMENTER.end(context, context.get(REQUEST_CONTEXT_KEY), null, error);
  }

  private static SpanKey[] getAllSpanKeys() {
    return new SpanKey[] {
      // span kind keys
      SpanKey.KIND_SERVER,
      SpanKey.KIND_CLIENT,
      SpanKey.KIND_CONSUMER,
      SpanKey.KIND_PRODUCER,
      // semantic convention keys
      SpanKey.HTTP_SERVER,
      SpanKey.RPC_SERVER,
      SpanKey.HTTP_CLIENT,
      SpanKey.RPC_CLIENT,
      SpanKey.DB_CLIENT,
      SpanKey.PRODUCER,
      SpanKey.CONSUMER_RECEIVE,
      SpanKey.CONSUMER_PROCESS,
      SpanKey.CONSUMER_SETTLE,
    };
  }

  private AgentSpanTestingInstrumenter() {}
}
