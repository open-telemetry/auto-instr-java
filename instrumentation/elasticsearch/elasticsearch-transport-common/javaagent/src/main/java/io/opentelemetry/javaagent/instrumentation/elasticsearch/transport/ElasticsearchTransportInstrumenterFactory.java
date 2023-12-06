/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.elasticsearch.transport;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.api.incubator.semconv.db.DbClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.db.DbClientSpanNameExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor;
import io.opentelemetry.javaagent.bootstrap.internal.InstrumentationConfig;
import org.elasticsearch.action.ActionResponse;

public final class ElasticsearchTransportInstrumenterFactory {
  private static final boolean CAPTURE_EXPERIMENTAL_SPAN_ATTRIBUTES =
      InstrumentationConfig.get()
          .getBoolean("otel.instrumentation.elasticsearch.experimental-span-attributes", false);

  public static Instrumenter<ElasticTransportRequest, ActionResponse> create(
      String instrumentationName,
      AttributesExtractor<ElasticTransportRequest, ActionResponse> experimentalAttributesExtractor,
      AttributesExtractor<ElasticTransportRequest, ActionResponse> netAttributesExtractor) {

    ElasticsearchTransportAttributeGetter dbClientAttributeGetter =
        new ElasticsearchTransportAttributeGetter();

    InstrumenterBuilder<ElasticTransportRequest, ActionResponse> instrumenterBuilder =
        Instrumenter.<ElasticTransportRequest, ActionResponse>builder(
                GlobalOpenTelemetry.get(),
                instrumentationName,
                DbClientSpanNameExtractor.create(dbClientAttributeGetter))
            .addAttributesExtractor(DbClientAttributesExtractor.create(dbClientAttributeGetter))
            .addAttributesExtractor(netAttributesExtractor);

    if (CAPTURE_EXPERIMENTAL_SPAN_ATTRIBUTES) {
      instrumenterBuilder.addAttributesExtractor(experimentalAttributesExtractor);
    }

    return instrumenterBuilder.buildInstrumenter(SpanKindExtractor.alwaysClient());
  }

  private ElasticsearchTransportInstrumenterFactory() {}
}
