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
import io.opentelemetry.instrumentation.api.semconv.network.internal.InternalNetworkAttributesExtractor;
import io.opentelemetry.instrumentation.api.semconv.network.internal.InternalServerAttributesExtractor;
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.UrlAttributes;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.ToIntFunction;
import javax.annotation.Nullable;

/**
 * Extractor of <a
 * href="https://github.com/open-telemetry/semantic-conventions/blob/v1.23.0/docs/http/http-spans.md#http-client">HTTP
 * client attributes</a>.
 *
 * @since 2.0.0
 */
public final class HttpClientAttributesExtractor<REQUEST, RESPONSE>
    extends HttpCommonAttributesExtractor<
        REQUEST, RESPONSE, HttpClientAttributesGetter<REQUEST, RESPONSE>>
    implements SpanKeyProvider {

  private static final Set<String> PARAMS_TO_REDACT =
      new HashSet<>(Arrays.asList("AWSAccessKeyId", "Signature", "sig", "X-Goog-Signature"));

  /**
   * Creates the HTTP client attributes extractor with default configuration.
   *
   * @see InstrumenterBuilder#addAttributesExtractor(AttributesExtractor)
   */
  public static <REQUEST, RESPONSE> AttributesExtractor<REQUEST, RESPONSE> create(
      HttpClientAttributesGetter<REQUEST, RESPONSE> httpAttributesGetter) {
    return builder(httpAttributesGetter).build();
  }

  /**
   * Returns a new {@link HttpClientAttributesExtractorBuilder} that can be used to configure the
   * HTTP client attributes extractor.
   */
  public static <REQUEST, RESPONSE> HttpClientAttributesExtractorBuilder<REQUEST, RESPONSE> builder(
      HttpClientAttributesGetter<REQUEST, RESPONSE> httpAttributesGetter) {
    return new HttpClientAttributesExtractorBuilder<>(httpAttributesGetter);
  }

  private final InternalNetworkAttributesExtractor<REQUEST, RESPONSE> internalNetworkExtractor;
  private final InternalServerAttributesExtractor<REQUEST> internalServerExtractor;
  private final ToIntFunction<Context> resendCountIncrementer;
  private final boolean redactSensitiveParameters;

  HttpClientAttributesExtractor(HttpClientAttributesExtractorBuilder<REQUEST, RESPONSE> builder) {
    super(
        builder.httpAttributesGetter,
        HttpStatusCodeConverter.CLIENT,
        builder.capturedRequestHeaders,
        builder.capturedResponseHeaders,
        builder.knownMethods);
    internalNetworkExtractor = builder.buildNetworkExtractor();
    internalServerExtractor = builder.buildServerExtractor();
    resendCountIncrementer = builder.resendCountIncrementer;
    redactSensitiveParameters =
        Boolean.getBoolean(
            "otel.instrumentation.http.client.experimental.redact-sensitive-parameters");
  }

  @Override
  public void onStart(AttributesBuilder attributes, Context parentContext, REQUEST request) {
    super.onStart(attributes, parentContext, request);

    internalServerExtractor.onStart(attributes, request);

    String fullUrl = stripSensitiveData(getter.getUrlFull(request));
    internalSet(attributes, UrlAttributes.URL_FULL, fullUrl);

    int resendCount = resendCountIncrementer.applyAsInt(parentContext);
    if (resendCount > 0) {
      attributes.put(HttpAttributes.HTTP_REQUEST_RESEND_COUNT, resendCount);
    }
  }

  @Override
  public void onEnd(
      AttributesBuilder attributes,
      Context context,
      REQUEST request,
      @Nullable RESPONSE response,
      @Nullable Throwable error) {
    super.onEnd(attributes, context, request, response, error);

    internalNetworkExtractor.onEnd(attributes, request, response);
  }

  /**
   * This method is internal and is hence not for public use. Its API is unstable and can change at
   * any time.
   */
  @Override
  public SpanKey internalGetSpanKey() {
    return SpanKey.HTTP_CLIENT;
  }

  @Nullable
  private String stripSensitiveData(@Nullable String url) {
    if (url == null || url.isEmpty()) {
      return url;
    }

    int schemeEndIndex = url.indexOf(':');

    if (schemeEndIndex == -1) {
      // not a valid url
      return url;
    }

    int len = url.length();
    if (len <= schemeEndIndex + 2
        || url.charAt(schemeEndIndex + 1) != '/'
        || url.charAt(schemeEndIndex + 2) != '/') {
      // has no authority component
      return url;
    }

    // look for the end of the authority component:
    //   '/', '?', '#' ==> start of path
    int index;
    int atIndex = -1;
    for (index = schemeEndIndex + 3; index < len; index++) {
      char c = url.charAt(index);

      if (c == '@') {
        atIndex = index;
      }

      if (c == '/' || c == '?' || c == '#') {
        break;
      }
    }

    if (atIndex == -1 || atIndex == len - 1) {
      return redactSensitiveParameters ? redactUrlParameters(url) : url;
    }

    String afterUserPwdRedaction = url.substring(atIndex);
    return url.substring(0, schemeEndIndex + 3)
        + "REDACTED:REDACTED"
        + (redactSensitiveParameters
            ? redactUrlParameters(afterUserPwdRedaction)
            : afterUserPwdRedaction);
  }

  private static String redactUrlParameters(String urlpart) {

    int questionMarkIndex = urlpart.indexOf('?');

    if (questionMarkIndex == -1) {
      return urlpart;
    }

    if (!containsParamToRedact(urlpart)) {
      return urlpart;
    }

    StringBuilder redactedParameters = new StringBuilder();
    boolean paramToRedact = false;
    boolean paramNameDetected = false;
    boolean reference = false;

    StringBuilder currentParamName = new StringBuilder();

    for (int i = questionMarkIndex + 1; i < urlpart.length(); i++) {
      char currentChar = urlpart.charAt(i);
      if (currentChar == '=') {
        paramNameDetected = true;
        redactedParameters.append(currentParamName);
        redactedParameters.append('=');
        if (PARAMS_TO_REDACT.contains(currentParamName.toString())) {
          redactedParameters.append("REDACTED");
          paramToRedact = true;
        }
      } else if (currentChar == '&') {
        redactedParameters.append('&');
        paramNameDetected = false;
        paramToRedact = false;
        currentParamName.setLength(0);
      } else if (currentChar == '#') {
        reference = true;
        redactedParameters.append('#');
      } else if (!paramNameDetected) {
        currentParamName.append(currentChar);
      } else if (!paramToRedact || reference) {
        redactedParameters.append(currentChar);
      }
    }
    return urlpart.substring(0, questionMarkIndex) + "?" + redactedParameters;
  }

  private static boolean containsParamToRedact(String urlpart) {
    for (String param : PARAMS_TO_REDACT) {
      if (urlpart.contains(param)) {
        return true;
      }
    }
    return false;
  }
}
