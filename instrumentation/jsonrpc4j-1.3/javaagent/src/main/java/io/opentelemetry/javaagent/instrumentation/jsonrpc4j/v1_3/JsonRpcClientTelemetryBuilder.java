/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.jsonrpc4j.v1_3;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.incubator.semconv.rpc.RpcClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.rpc.RpcClientMetrics;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import java.util.ArrayList;
import java.util.List;

public class JsonRpcClientTelemetryBuilder {

  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.jsonrpc4j-1.3";

  private final OpenTelemetry openTelemetry;

  private final List<
          AttributesExtractor<? super JsonRpcClientRequest, ? super JsonRpcClientResponse>>
      additionalClientExtractors = new ArrayList<>();

  JsonRpcClientTelemetryBuilder(OpenTelemetry openTelemetry) {
    this.openTelemetry = openTelemetry;
  }

  /**
   * Adds an extra client-only {@link AttributesExtractor} to invoke to set attributes to
   * instrumented items. The {@link AttributesExtractor} will be executed after all default
   * extractors.
   */
  @CanIgnoreReturnValue
  public JsonRpcClientTelemetryBuilder addClientAttributeExtractor(
      AttributesExtractor<? super JsonRpcClientRequest, ? super JsonRpcClientResponse>
          attributesExtractor) {
    additionalClientExtractors.add(attributesExtractor);
    return this;
  }

  public JsonRpcClientTelemetry build() {
    SpanNameExtractor<JsonRpcClientRequest> clientSpanNameExtractor =
        new JsonRpcClientSpanNameExtractor();

    InstrumenterBuilder<JsonRpcClientRequest, JsonRpcClientResponse> clientInstrumenterBuilder =
        Instrumenter.builder(openTelemetry, INSTRUMENTATION_NAME, clientSpanNameExtractor);

    JsonRpcClientAttributesGetter clientRpcAttributesGetter =
        JsonRpcClientAttributesGetter.INSTANCE;

    clientInstrumenterBuilder
        .addAttributesExtractor(RpcClientAttributesExtractor.create(clientRpcAttributesGetter))
        .addAttributesExtractors(additionalClientExtractors)
        .addAttributesExtractor(new JsonRpcClientAttributesExtractor())
        .addOperationMetrics(RpcClientMetrics.get());

    return new JsonRpcClientTelemetry(
        clientInstrumenterBuilder.buildInstrumenter(SpanKindExtractor.alwaysClient()));
  }
}
