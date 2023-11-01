/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.okhttp.v3_0;

import com.google.errorprone.annotations.CanIgnoreReturnValue;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientAttributesExtractorBuilder;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientExperimentalMetrics;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientSemanticConvention;
import io.opentelemetry.instrumentation.okhttp.v3_0.internal.OkHttpAttributesGetter;
import java.util.List;
import java.util.Set;
import okhttp3.Request;
import okhttp3.Response;

/** A builder of {@link OkHttpTelemetry}. */
public final class OkHttpTelemetryBuilder {

  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.okhttp-3.0";

  private final OpenTelemetry openTelemetry;
  private final HttpClientSemanticConvention<Request, Response> semanticConvention;
  private boolean emitExperimentalHttpClientMetrics = false;

  OkHttpTelemetryBuilder(OpenTelemetry openTelemetry) {
    this.openTelemetry = openTelemetry;
    semanticConvention =
        HttpClientSemanticConvention.create(
            openTelemetry, INSTRUMENTATION_NAME, OkHttpAttributesGetter.INSTANCE);
  }

  /**
   * Adds an additional {@link AttributesExtractor} to invoke to set attributes to instrumented
   * items.
   */
  @CanIgnoreReturnValue
  public OkHttpTelemetryBuilder addAttributesExtractor(
      AttributesExtractor<Request, Response> attributesExtractor) {
    semanticConvention.configureInstrumenter(
        builder -> builder.addAttributesExtractor(attributesExtractor));
    return this;
  }

  /**
   * Configures the HTTP request headers that will be captured as span attributes.
   *
   * @param requestHeaders A list of HTTP header names.
   */
  @CanIgnoreReturnValue
  public OkHttpTelemetryBuilder setCapturedRequestHeaders(List<String> requestHeaders) {
    semanticConvention.setCapturedRequestHeaders(requestHeaders);
    return this;
  }

  /**
   * Configures the HTTP response headers that will be captured as span attributes.
   *
   * @param responseHeaders A list of HTTP header names.
   */
  @CanIgnoreReturnValue
  public OkHttpTelemetryBuilder setCapturedResponseHeaders(List<String> responseHeaders) {
    semanticConvention.setCapturedResponseHeaders(responseHeaders);
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
   * @see HttpClientAttributesExtractorBuilder#setKnownMethods(Set)
   */
  @CanIgnoreReturnValue
  public OkHttpTelemetryBuilder setKnownMethods(Set<String> knownMethods) {
    semanticConvention.setKnownMethods(knownMethods);
    return this;
  }

  /**
   * Configures the instrumentation to emit experimental HTTP client metrics.
   *
   * @param emitExperimentalHttpClientMetrics {@code true} if the experimental HTTP client metrics
   *     are to be emitted.
   */
  @CanIgnoreReturnValue
  public OkHttpTelemetryBuilder setEmitExperimentalHttpClientMetrics(
      boolean emitExperimentalHttpClientMetrics) {
    this.emitExperimentalHttpClientMetrics = emitExperimentalHttpClientMetrics;
    return this;
  }

  /**
   * Returns a new {@link OkHttpTelemetry} with the settings of this {@link OkHttpTelemetryBuilder}.
   */
  public OkHttpTelemetry build() {
    if (emitExperimentalHttpClientMetrics) {
      semanticConvention.configureInstrumenter(
          builder -> builder.addOperationMetrics(HttpClientExperimentalMetrics.get()));
    }
    return new OkHttpTelemetry(
        semanticConvention.buildInstrumenter(), openTelemetry.getPropagators());
  }
}
