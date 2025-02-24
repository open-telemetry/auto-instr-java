/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.jsonrpc4j.v1_3;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.incubator.semconv.rpc.RpcServerAttributesExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.rpc.RpcServerMetrics;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import java.util.ArrayList;
import java.util.List;

public class JsonRpcServerTelemetryBuilder {

  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.jsonrpc4j-1.3";

  private final OpenTelemetry openTelemetry;

  private final List<
          AttributesExtractor<? super JsonRpcServerRequest, ? super JsonRpcServerResponse>>
      additionalServerExtractors = new ArrayList<>();

  JsonRpcServerTelemetryBuilder(OpenTelemetry openTelemetry) {
    this.openTelemetry = openTelemetry;
  }

  /**
   * Adds an extra server-only {@link AttributesExtractor} to invoke to set attributes to
   * instrumented items. The {@link AttributesExtractor} will be executed after all default
   * extractors.
   */
  @CanIgnoreReturnValue
  public JsonRpcServerTelemetryBuilder addServerAttributeExtractor(
      AttributesExtractor<? super JsonRpcServerRequest, ? super JsonRpcServerResponse>
          attributesExtractor) {
    additionalServerExtractors.add(attributesExtractor);
    return this;
  }

  public JsonRpcServerTelemetry build() {
    SpanNameExtractor<JsonRpcServerRequest> serverSpanNameExtractor =
        new JsonRpcServerSpanNameExtractor();

    InstrumenterBuilder<JsonRpcServerRequest, JsonRpcServerResponse> serverInstrumenterBuilder =
        Instrumenter.builder(openTelemetry, INSTRUMENTATION_NAME, serverSpanNameExtractor);

    JsonRpcServerAttributesGetter serverRpcAttributesGetter =
        JsonRpcServerAttributesGetter.INSTANCE;

    serverInstrumenterBuilder
        .setSpanStatusExtractor(JsonRpcServerSpanStatusExtractor.INSTANCE)
        .addAttributesExtractor(RpcServerAttributesExtractor.create(serverRpcAttributesGetter))
        .addAttributesExtractor(new JsonRpcServerAttributesExtractor())
        .addAttributesExtractors(additionalServerExtractors)
        .addOperationMetrics(RpcServerMetrics.get());

    return new JsonRpcServerTelemetry(
        serverInstrumenterBuilder.buildServerInstrumenter(JsonRpcServerRequestGetter.INSTANCE));
  }
}
