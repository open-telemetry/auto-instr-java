/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.elasticsearch.rest.v7_0;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.SpanNameExtractor;
import io.opentelemetry.instrumentation.api.internal.HttpConstants;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientAttributesExtractorBuilder;
import io.opentelemetry.instrumentation.elasticsearch.rest.common.v5_0.internal.ElasticsearchRestInstrumenterFactory;
import io.opentelemetry.instrumentation.elasticsearch.rest.common.v5_0.internal.ElasticsearchRestRequest;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.elasticsearch.client.Response;

public final class ElasticsearchRest7TelemetryBuilder {

  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.elasticsearch-rest-7.0";

  private final OpenTelemetry openTelemetry;
  private final List<AttributesExtractor<ElasticsearchRestRequest, Response>> attributesExtractors =
      new ArrayList<>();
  private Set<String> knownMethods = HttpConstants.KNOWN_METHODS;
  private Function<
          SpanNameExtractor<ElasticsearchRestRequest>,
          ? extends SpanNameExtractor<? super ElasticsearchRestRequest>>
      spanNameExtractorTransformer = Function.identity();

  ElasticsearchRest7TelemetryBuilder(OpenTelemetry openTelemetry) {
    this.openTelemetry = openTelemetry;
  }

  /**
   * Adds an additional {@link AttributesExtractor} to invoke to set attributes to instrumented
   * items.
   */
  @CanIgnoreReturnValue
  public ElasticsearchRest7TelemetryBuilder addAttributesExtractor(
      AttributesExtractor<ElasticsearchRestRequest, Response> attributesExtractor) {
    attributesExtractors.add(attributesExtractor);
    return this;
  }

  /**
   * Configures the instrumentation to recognize an alternative set of HTTP request methods.
   *
   * <p>By default, this instrumentation defines "known" methods as the ones listed in <a
   * href="https://www.rfc-editor.org/rfc/rfc9110.html#name-methods">RFC9110</a> and the PATCH
   * method defined in <a href="https://www.rfc-editor.org/rfc/rfc5789.html">RFC5789</a>.
   *
   * <p>Note: calling this method <b>overrides</b> the default known method sets completely; it does
   * not supplement it.
   *
   * @param knownMethods A set of recognized HTTP request methods.
   * @see HttpClientAttributesExtractorBuilder#setKnownMethods(Collection)
   */
  @CanIgnoreReturnValue
  public ElasticsearchRest7TelemetryBuilder setKnownMethods(Collection<String> knownMethods) {
    this.knownMethods = new HashSet<>(knownMethods);
    return this;
  }

  /** Sets custom {@link SpanNameExtractor} via transform function. */
  @CanIgnoreReturnValue
  public ElasticsearchRest7TelemetryBuilder setSpanNameExtractor(
      Function<
              SpanNameExtractor<ElasticsearchRestRequest>,
              ? extends SpanNameExtractor<? super ElasticsearchRestRequest>>
          spanNameExtractorTransformer) {
    this.spanNameExtractorTransformer = spanNameExtractorTransformer;
    return this;
  }

  /**
   * Returns a new {@link ElasticsearchRest7Telemetry} with the settings of this {@link
   * ElasticsearchRest7TelemetryBuilder}.
   */
  public ElasticsearchRest7Telemetry build() {
    Instrumenter<ElasticsearchRestRequest, Response> instrumenter =
        ElasticsearchRestInstrumenterFactory.create(
            openTelemetry,
            INSTRUMENTATION_NAME,
            attributesExtractors,
            spanNameExtractorTransformer,
            knownMethods,
            false);

    return new ElasticsearchRest7Telemetry(instrumenter);
  }
}
