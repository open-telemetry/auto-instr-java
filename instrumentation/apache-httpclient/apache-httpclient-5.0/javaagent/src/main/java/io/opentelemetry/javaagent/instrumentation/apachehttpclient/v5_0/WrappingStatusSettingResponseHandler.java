/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.v5_0;

import static io.opentelemetry.javaagent.instrumentation.apachehttpclient.v5_0.ApacheHttpClientSingletons.helper;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import java.io.IOException;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;

public final class WrappingStatusSettingResponseHandler<T> implements HttpClientResponseHandler<T> {
  final Context context;
  final Context parentContext;
  final ApacheHttpClientRequest otelRequest;
  final HttpClientResponseHandler<T> handler;

  public WrappingStatusSettingResponseHandler(
      Context context,
      Context parentContext,
      ApacheHttpClientRequest otelRequest,
      HttpClientResponseHandler<T> handler) {
    this.context = context;
    this.parentContext = parentContext;
    this.otelRequest = otelRequest;
    this.handler = handler;
  }

  @Override
  public T handleResponse(ClassicHttpResponse response) throws IOException, HttpException {
    helper().endInstrumentation(context, otelRequest, response, null);
    // ending the span before executing the callback handler (and scoping the callback handler to
    // the parent context), even though we are inside the synchronous http client callback
    // underneath HttpClient.execute(..), in order to not attribute other CLIENT span timings that
    // may be performed in the callback handler to the http client span (and so we don't end up with
    // nested CLIENT spans, which we currently suppress)
    try (Scope ignored = parentContext.makeCurrent()) {
      return handler.handleResponse(response);
    }
  }
}
