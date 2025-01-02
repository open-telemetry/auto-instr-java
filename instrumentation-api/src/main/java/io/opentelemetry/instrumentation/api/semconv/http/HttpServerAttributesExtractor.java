/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.semconv.http;

import static io.opentelemetry.instrumentation.api.internal.AttributesExtractorUtil.internalSet;

import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.instrumenter.InstrumenterBuilder;
import io.opentelemetry.instrumentation.api.internal.SpanKey;
import io.opentelemetry.instrumentation.api.internal.SpanKeyProvider;
import io.opentelemetry.instrumentation.api.semconv.network.internal.InternalClientAttributesExtractor;
import io.opentelemetry.instrumentation.api.semconv.network.internal.InternalNetworkAttributesExtractor;
import io.opentelemetry.instrumentation.api.semconv.network.internal.InternalServerAttributesExtractor;
import io.opentelemetry.instrumentation.api.semconv.url.internal.InternalUrlAttributesExtractor;
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.UserAgentAttributes;
import java.util.function.Function;
import javax.annotation.Nullable;

/**
 * Extractor of <a
 * href="https://github.com/open-telemetry/semantic-conventions/blob/v1.23.0/docs/http/http-spans.md#http-server">HTTP
 * server attributes</a>.
 *
 * @since 2.0.0
 */
public final class HttpServerAttributesExtractor<REQUEST, RESPONSE>
    extends HttpCommonAttributesExtractor<
        REQUEST, RESPONSE, HttpServerAttributesGetter<REQUEST, RESPONSE>>
    implements SpanKeyProvider {

  /**
   * Creates the HTTP server attributes extractor with default configuration.
   *
   * @see InstrumenterBuilder#addAttributesExtractor(AttributesExtractor)
   */
  public static <REQUEST, RESPONSE> AttributesExtractor<REQUEST, RESPONSE> create(
      HttpServerAttributesGetter<REQUEST, RESPONSE> httpAttributesGetter) {
    return builder(httpAttributesGetter).build();
  }

  /**
   * Returns a new {@link HttpServerAttributesExtractorBuilder} that can be used to configure the
   * HTTP client attributes extractor.
   */
  public static <REQUEST, RESPONSE> HttpServerAttributesExtractorBuilder<REQUEST, RESPONSE> builder(
      HttpServerAttributesGetter<REQUEST, RESPONSE> httpAttributesGetter) {
    return new HttpServerAttributesExtractorBuilder<>(httpAttributesGetter);
  }

  private final InternalUrlAttributesExtractor<REQUEST> internalUrlExtractor;
  private final InternalNetworkAttributesExtractor<REQUEST, RESPONSE> internalNetworkExtractor;
  private final InternalServerAttributesExtractor<REQUEST> internalServerExtractor;
  private final InternalClientAttributesExtractor<REQUEST> internalClientExtractor;
  private final Function<Context, String> httpRouteGetter;

  HttpServerAttributesExtractor(HttpServerAttributesExtractorBuilder<REQUEST, RESPONSE> builder) {
    super(
        builder.httpAttributesGetter,
        HttpStatusCodeConverter.SERVER,
        builder.capturedRequestHeaders,
        builder.capturedResponseHeaders,
        builder.knownMethods);
    internalUrlExtractor = builder.buildUrlExtractor();
    internalNetworkExtractor = builder.buildNetworkExtractor();
    internalServerExtractor = builder.buildServerExtractor();
    internalClientExtractor = builder.buildClientExtractor();
    httpRouteGetter = builder.httpRouteGetter;
  }

  @Override
  public void onStart(AttributesBuilder attributes, Context parentContext, REQUEST request) {
    super.onStart(attributes, parentContext, request);
  }

  @Override
  public void onEnd(
      AttributesBuilder attributes,
      Context context,
      REQUEST request,
      @Nullable RESPONSE response,
      @Nullable Throwable error) {

    super.onEnd(attributes, context, request, response, error);
    internalSet(attributes, UserAgentAttributes.USER_AGENT_ORIGINAL, userAgent(request));

    internalUrlExtractor.onEnd(attributes, request);
    internalServerExtractor.onEnd(attributes, request);
    internalClientExtractor.onEnd(attributes, request);

    internalSet(attributes, HttpAttributes.HTTP_ROUTE, getter.getHttpRoute(request));
    internalNetworkExtractor.onEnd(attributes, request, response);

    internalSet(attributes, HttpAttributes.HTTP_ROUTE, httpRouteGetter.apply(context));
  }

  /**
   * This method is internal and is hence not for public use. Its API is unstable and can change at
   * any time.
   */
  @Override
  public SpanKey internalGetSpanKey() {
    return SpanKey.HTTP_SERVER;
  }

  @Nullable
  private String userAgent(REQUEST request) {
    return firstHeaderValue(getter.getHttpRequestHeader(request, "user-agent"));
  }
}
