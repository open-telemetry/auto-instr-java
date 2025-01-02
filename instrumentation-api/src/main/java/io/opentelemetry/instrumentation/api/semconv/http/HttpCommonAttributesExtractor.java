/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.semconv.http;

import static io.opentelemetry.instrumentation.api.internal.AttributesExtractorUtil.internalSet;
import static io.opentelemetry.instrumentation.api.internal.HttpConstants._OTHER;
import static io.opentelemetry.instrumentation.api.semconv.http.CapturedHttpHeadersUtil.lowercase;
import static io.opentelemetry.instrumentation.api.semconv.http.CapturedHttpHeadersUtil.requestAttributeKey;
import static io.opentelemetry.instrumentation.api.semconv.http.CapturedHttpHeadersUtil.responseAttributeKey;

import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.AttributesExtractor;
import io.opentelemetry.instrumentation.api.semconv.network.NetworkAttributesGetter;
import io.opentelemetry.semconv.ErrorAttributes;
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.NetworkAttributes;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import javax.annotation.Nullable;

/**
 * Extractor of <a
 * href="https://github.com/open-telemetry/semantic-conventions/blob/v1.23.0/docs/http/http-spans.md#common-attributes">HTTP
 * attributes</a> that are common to client and server instrumentations.
 */
abstract class HttpCommonAttributesExtractor<
        REQUEST,
        RESPONSE,
        GETTER extends
            HttpCommonAttributesGetter<REQUEST, RESPONSE>
                & NetworkAttributesGetter<REQUEST, RESPONSE>>
    implements AttributesExtractor<REQUEST, RESPONSE> {

  final GETTER getter;
  private final HttpStatusCodeConverter statusCodeConverter;
  private final String[] capturedRequestHeaders;
  private final String[] capturedResponseHeaders;
  private final Set<String> knownMethods;

  HttpCommonAttributesExtractor(
      GETTER getter,
      HttpStatusCodeConverter statusCodeConverter,
      List<String> capturedRequestHeaders,
      List<String> capturedResponseHeaders,
      Set<String> knownMethods) {
    this.getter = getter;
    this.statusCodeConverter = statusCodeConverter;
    this.capturedRequestHeaders = lowercase(capturedRequestHeaders);
    this.capturedResponseHeaders = lowercase(capturedResponseHeaders);
    this.knownMethods = new HashSet<>(knownMethods);
  }

  @Override
  public void onStart(AttributesBuilder attributes, Context parentContext, REQUEST request) {}

  @Override
  public void onEnd(
      AttributesBuilder attributes,
      Context context,
      REQUEST request,
      @Nullable RESPONSE response,
      @Nullable Throwable error) {
    String method = getter.getHttpRequestMethod(request);
    if (method == null || knownMethods.contains(method)) {
      internalSet(attributes, HttpAttributes.HTTP_REQUEST_METHOD, method);
    } else {
      internalSet(attributes, HttpAttributes.HTTP_REQUEST_METHOD, _OTHER);
      internalSet(attributes, HttpAttributes.HTTP_REQUEST_METHOD_ORIGINAL, method);
    }

    for (String name : capturedRequestHeaders) {
      List<String> values = getter.getHttpRequestHeader(request, name);
      if (!values.isEmpty()) {
        internalSet(attributes, requestAttributeKey(name), values);
      }
    }

    Integer statusCode = null;
    if (response != null) {
      statusCode = getter.getHttpResponseStatusCode(request, response, error);
      if (statusCode != null && statusCode > 0) {
        internalSet(attributes, HttpAttributes.HTTP_RESPONSE_STATUS_CODE, (long) statusCode);
      }

      for (String name : capturedResponseHeaders) {
        List<String> values = getter.getHttpResponseHeader(request, response, name);
        if (!values.isEmpty()) {
          internalSet(attributes, responseAttributeKey(name), values);
        }
      }
    }

    String errorType = null;
    if (statusCode != null && statusCode > 0) {
      if (statusCodeConverter.isError(statusCode)) {
        errorType = statusCode.toString();
      }
    } else {
      errorType = getter.getErrorType(request, response, error);
      // fall back to exception class name & _OTHER
      if (errorType == null && error != null) {
        errorType = error.getClass().getName();
      }
      if (errorType == null) {
        errorType = _OTHER;
      }
    }
    internalSet(attributes, ErrorAttributes.ERROR_TYPE, errorType);

    String protocolName = lowercaseStr(getter.getNetworkProtocolName(request, response));
    String protocolVersion = lowercaseStr(getter.getNetworkProtocolVersion(request, response));

    if (protocolVersion != null) {
      if (!"http".equals(protocolName)) {
        internalSet(attributes, NetworkAttributes.NETWORK_PROTOCOL_NAME, protocolName);
      }
      internalSet(attributes, NetworkAttributes.NETWORK_PROTOCOL_VERSION, protocolVersion);
    }
  }

  @Nullable
  static String firstHeaderValue(List<String> values) {
    return values.isEmpty() ? null : values.get(0);
  }

  @Nullable
  private static String lowercaseStr(@Nullable String str) {
    return str == null ? null : str.toLowerCase(Locale.ROOT);
  }
}
