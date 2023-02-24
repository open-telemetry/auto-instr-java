/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.v5_0;

import static io.opentelemetry.javaagent.instrumentation.apachehttpclient.v5_0.ApacheHttpClientContextManager.httpContextManager;
import static io.opentelemetry.javaagent.instrumentation.apachehttpclient.v5_0.ApacheHttpClientSingletons.helper;

import io.opentelemetry.context.Context;
import java.io.IOException;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.nio.RequestChannel;
import org.apache.hc.core5.http.protocol.HttpContext;

public final class WrappedRequestChannel implements RequestChannel {
  private final RequestChannel delegate;
  private final Context parentContext;
  private final WrappedFutureCallback<?> wrappedFutureCallback;

  public WrappedRequestChannel(
      RequestChannel requestChannel,
      Context parentContext,
      WrappedFutureCallback<?> wrappedFutureCallback) {
    this.delegate = requestChannel;
    this.parentContext = parentContext;
    this.wrappedFutureCallback = wrappedFutureCallback;
  }

  @Override
  public void sendRequest(HttpRequest request, EntityDetails entityDetails, HttpContext httpContext)
      throws HttpException, IOException {
    ApacheHttpClientRequest otelRequest = new ApacheHttpClientRequest(parentContext, request);
    Context context = helper().startInstrumentation(parentContext, request, otelRequest);

    if (context != null) {
      wrappedFutureCallback.context = context;
      wrappedFutureCallback.otelRequest = otelRequest;

      // As the http processor instrumentation is going to be called asynchronously,
      // we will need to store the otel context variables in http context for the
      // http processor instrumentation to use
      httpContextManager().setCurrentContext(httpContext, context);
    }

    delegate.sendRequest(request, entityDetails, httpContext);
  }
}
