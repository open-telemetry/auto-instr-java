/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.tomcat.common;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.api.incubator.semconv.http.HttpExperimentalAttributesExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.http.HttpServerExperimentalMetrics;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.semconv.http.HttpServerAttributesExtractor;
import io.opentelemetry.instrumentation.api.semconv.http.HttpServerMetrics;
import io.opentelemetry.instrumentation.api.semconv.http.HttpServerRoute;
import io.opentelemetry.instrumentation.api.semconv.http.HttpSpanNameExtractor;
import io.opentelemetry.instrumentation.api.semconv.http.HttpSpanStatusExtractor;
import io.opentelemetry.javaagent.bootstrap.internal.CommonConfig;
import io.opentelemetry.javaagent.bootstrap.servlet.AppServerBridge;
import io.opentelemetry.javaagent.instrumentation.servlet.ServletAccessor;
import io.opentelemetry.javaagent.instrumentation.servlet.ServletErrorCauseExtractor;
import org.apache.coyote.Request;
import org.apache.coyote.Response;

public final class TomcatInstrumenterFactory {

  private TomcatInstrumenterFactory() {}

  public static <REQUEST, RESPONSE> Instrumenter<Request, Response> create(
      String instrumentationName, ServletAccessor<REQUEST, RESPONSE> accessor) {
    TomcatHttpAttributeGetter httpAttributeGetter = new TomcatHttpAttributeGetter();

    InstrumenterBuilder<Request, Response> builder =
        Instrumenter.<Request, Response>builder(
                GlobalOpenTelemetry.get(),
                instrumentationName,
                HttpSpanNameExtractor.builder(httpAttributeGetter)
                    .setKnownMethods(CommonConfig.get().getKnownHttpRequestMethods())
                    .build())
            .setSpanStatusExtractor(HttpSpanStatusExtractor.create(httpAttributeGetter))
            .setErrorCauseExtractor(new ServletErrorCauseExtractor<>(accessor))
            .addAttributesExtractor(
                HttpServerAttributesExtractor.builder(httpAttributeGetter)
                    .setCapturedRequestHeaders(CommonConfig.get().getServerRequestHeaders())
                    .setCapturedResponseHeaders(CommonConfig.get().getServerResponseHeaders())
                    .setKnownMethods(CommonConfig.get().getKnownHttpRequestMethods())
                    .build())
            .addContextCustomizer(
                HttpServerRoute.builder(httpAttributeGetter)
                    .setKnownMethods(CommonConfig.get().getKnownHttpRequestMethods())
                    .build())
            .addContextCustomizer(
                (context, request, attributes) ->
                    new AppServerBridge.Builder()
                        .captureServletAttributes()
                        .recordException()
                        .init(context))
            .addOperationMetrics(HttpServerMetrics.get());
    if (CommonConfig.get().shouldEmitExperimentalHttpServerTelemetry()) {
      builder
          .addAttributesExtractor(HttpExperimentalAttributesExtractor.create(httpAttributeGetter))
          .addOperationMetrics(HttpServerExperimentalMetrics.get());
    }
    return builder.buildServerInstrumenter(TomcatRequestGetter.INSTANCE);
  }
}
