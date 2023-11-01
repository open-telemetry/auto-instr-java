/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.instrumenter.http;

import static io.opentelemetry.instrumentation.api.instrumenter.SpanKindExtractor.alwaysClient;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.SpanStatusExtractor;
import io.opentelemetry.instrumentation.api.internal.HttpConstants;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

public final class HttpClientSemanticConvention<REQUEST, RESPONSE> {

  public static <REQUEST, RESPONSE> HttpClientSemanticConvention<REQUEST, RESPONSE> create(
      OpenTelemetry openTelemetry,
      String instrumentationName,
      HttpClientAttributesGetter<REQUEST, RESPONSE> getter) {
    return new HttpClientSemanticConvention<>(openTelemetry, instrumentationName, getter);
  }

  final OpenTelemetry openTelemetry;
  final String instrumentationName;
  final HttpClientAttributesGetter<REQUEST, RESPONSE> getter;

  final HttpSpanNameExtractorBuilder<REQUEST> spanNameExtractorBuilder;
  final HttpClientAttributesExtractorBuilder<REQUEST, RESPONSE> attributesExtractorBuilder;
  final SpanStatusExtractor<REQUEST, RESPONSE> spanStatusExtractor;

  Consumer<InstrumenterBuilder<REQUEST, RESPONSE>> instrumenterConfigurer = builder -> {};

  private HttpClientSemanticConvention(
      OpenTelemetry openTelemetry,
      String instrumentationName,
      HttpClientAttributesGetter<REQUEST, RESPONSE> getter) {
    this.openTelemetry = openTelemetry;
    this.instrumentationName = instrumentationName;
    this.getter = getter;

    spanNameExtractorBuilder = HttpSpanNameExtractor.builder(getter);
    spanStatusExtractor = HttpSpanStatusExtractor.create(getter);
    attributesExtractorBuilder = HttpClientAttributesExtractor.builder(getter);
  }

  @CanIgnoreReturnValue
  public HttpClientSemanticConvention<REQUEST, RESPONSE> configureInstrumenter(
      Consumer<InstrumenterBuilder<REQUEST, RESPONSE>> instrumenterConfigurer) {
    this.instrumenterConfigurer = this.instrumenterConfigurer.andThen(instrumenterConfigurer);
    return this;
  }

  /**
   * Configures the HTTP request headers that will be captured as span attributes as described in <a
   * href="https://github.com/open-telemetry/semantic-conventions/blob/main/docs/http/http-spans.md#common-attributes">HTTP
   * semantic conventions</a>.
   *
   * <p>The HTTP request header values will be captured under the {@code http.request.header.<name>}
   * attribute key. The {@code <name>} part in the attribute key is the normalized header name:
   * lowercase, with dashes replaced by underscores.
   *
   * @param requestHeaders A list of HTTP header names.
   */
  @CanIgnoreReturnValue
  public HttpClientSemanticConvention<REQUEST, RESPONSE> setCapturedRequestHeaders(
      List<String> requestHeaders) {
    attributesExtractorBuilder.setCapturedRequestHeaders(requestHeaders);
    return this;
  }

  /**
   * Configures the HTTP response headers that will be captured as span attributes as described in
   * <a
   * href="https://github.com/open-telemetry/semantic-conventions/blob/main/docs/http/http-spans.md#common-attributes">HTTP
   * semantic conventions</a>.
   *
   * <p>The HTTP response header values will be captured under the {@code
   * http.response.header.<name>} attribute key. The {@code <name>} part in the attribute key is the
   * normalized header name: lowercase, with dashes replaced by underscores.
   *
   * @param responseHeaders A list of HTTP header names.
   */
  @CanIgnoreReturnValue
  public HttpClientSemanticConvention<REQUEST, RESPONSE> setCapturedResponseHeaders(
      List<String> responseHeaders) {
    attributesExtractorBuilder.setCapturedResponseHeaders(responseHeaders);
    return this;
  }

  /**
   * Configures the extractor to recognize an alternative set of HTTP request methods.
   *
   * <p>By default, this extractor defines "known" methods as the ones listed in <a
   * href="https://www.rfc-editor.org/rfc/rfc9110.html#name-methods">RFC9110</a> and the PATCH
   * method defined in <a href="https://www.rfc-editor.org/rfc/rfc5789.html">RFC5789</a>. If an
   * unknown method is encountered, the extractor will use the value {@value HttpConstants#_OTHER}
   * instead of it and put the original value in an extra {@code http.request.method_original}
   * attribute.
   *
   * <p>Note: calling this method <b>overrides</b> the default known method sets completely; it does
   * not supplement it.
   *
   * @param knownMethods A set of recognized HTTP request methods.
   */
  @CanIgnoreReturnValue
  public HttpClientSemanticConvention<REQUEST, RESPONSE> setKnownMethods(Set<String> knownMethods) {
    spanNameExtractorBuilder.setKnownMethods(knownMethods);
    attributesExtractorBuilder.setKnownMethods(knownMethods);
    return this;
  }

  public Instrumenter<REQUEST, RESPONSE> buildInstrumenter(TextMapSetter<REQUEST> setter) {
    return buildInstrumenter(builder -> builder.buildClientInstrumenter(setter));
  }

  public Instrumenter<REQUEST, RESPONSE> buildInstrumenter() {
    return buildInstrumenter(builder -> builder.buildInstrumenter(alwaysClient()));
  }

  private Instrumenter<REQUEST, RESPONSE> buildInstrumenter(
      Function<InstrumenterBuilder<REQUEST, RESPONSE>, Instrumenter<REQUEST, RESPONSE>>
          constructor) {

    InstrumenterBuilder<REQUEST, RESPONSE> builder =
        Instrumenter.<REQUEST, RESPONSE>builder(
                openTelemetry, instrumentationName, spanNameExtractorBuilder.build())
            .addAttributesExtractor(attributesExtractorBuilder.build())
            .setSpanStatusExtractor(spanStatusExtractor)
            .addOperationMetrics(HttpClientMetrics.get());

    instrumenterConfigurer.accept(builder);

    return constructor.apply(builder);
  }
}
