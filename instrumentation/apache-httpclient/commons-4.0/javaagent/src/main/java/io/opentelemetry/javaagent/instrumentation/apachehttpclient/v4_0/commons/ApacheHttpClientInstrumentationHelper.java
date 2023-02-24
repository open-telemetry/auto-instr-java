/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.v4_0.commons;

import static io.opentelemetry.javaagent.instrumentation.apachehttpclient.commons.BytesTransferMetrics.createOrGetWithParentContext;
import static io.opentelemetry.javaagent.instrumentation.apachehttpclient.v4_0.commons.ApacheHttpClientInernalEntityStorage.storage;

import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.javaagent.instrumentation.apachehttpclient.commons.BytesTransferMetrics;
import io.opentelemetry.javaagent.instrumentation.apachehttpclient.commons.OtelHttpRequest;
import io.opentelemetry.javaagent.instrumentation.apachehttpclient.commons.OtelHttpResponse;
import javax.annotation.Nullable;
import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

public final class ApacheHttpClientInstrumentationHelper {
  private final Instrumenter<OtelHttpRequest, OtelHttpResponse> instrumenter;

  public ApacheHttpClientInstrumentationHelper(
      Instrumenter<OtelHttpRequest, OtelHttpResponse> instrumenter) {
    this.instrumenter = instrumenter;
  }

  @Nullable
  public Context startInstrumentation(
      Context parentContext, HttpRequest request, ApacheHttpClientRequest otelRequest) {
    if (!instrumenter.shouldStart(parentContext, otelRequest)) {
      return null;
    }

    if (request instanceof HttpEntityEnclosingRequest) {
      HttpEntity originalEntity = ((HttpEntityEnclosingRequest) request).getEntity();
      if (originalEntity != null && originalEntity.isChunked()) {
        BytesTransferMetrics metrics = createOrGetWithParentContext(parentContext);
        HttpEntity wrappedHttpEntity = new WrappedHttpEntity(metrics, originalEntity);
        ((HttpEntityEnclosingRequest) request).setEntity(wrappedHttpEntity);
      }
    }

    return instrumenter.start(parentContext, otelRequest);
  }

  public <T> void endInstrumentation(
      Context context, ApacheHttpClientRequest otelRequest, T result, Throwable throwable) {
    OtelHttpRequest finalRequest = getFinalRequest(otelRequest, context);
    OtelHttpResponse finalResponse = getFinalResponse(result, context);
    instrumenter.end(context, finalRequest, finalResponse, throwable);
  }

  private static OtelHttpRequest getFinalRequest(ApacheHttpClientRequest request, Context context) {
    HttpRequest internalRequest = storage().getInternalRequest(context);
    if (internalRequest != null) {
      return request.withHttpRequest(internalRequest);
    }
    return request;
  }

  private static <T> OtelHttpResponse getFinalResponse(T result, Context context) {
    HttpResponse internalResponse = storage().getInternalResponse(context);
    if (internalResponse != null) {
      return new ApacheHttpClientResponse(internalResponse);
    }
    if (result instanceof HttpResponse) {
      return new ApacheHttpClientResponse((HttpResponse) result);
    }
    return null;
  }
}
