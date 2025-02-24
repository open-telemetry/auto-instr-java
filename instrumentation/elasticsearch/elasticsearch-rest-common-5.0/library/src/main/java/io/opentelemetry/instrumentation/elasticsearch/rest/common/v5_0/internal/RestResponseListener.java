/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.elasticsearch.rest.common.v5_0.internal;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class RestResponseListener implements ResponseListener {

  private final ResponseListener listener;
  private final Context parentContext;
  private final Instrumenter<ElasticsearchRestRequest, Response> instrumenter;
  private final Context context;
  private final ElasticsearchRestRequest request;

  public RestResponseListener(
      ResponseListener listener,
      Context parentContext,
      Instrumenter<ElasticsearchRestRequest, Response> instrumenter,
      Context context,
      ElasticsearchRestRequest request) {
    this.listener = listener;
    this.parentContext = parentContext;
    this.instrumenter = instrumenter;
    this.context = context;
    this.request = request;
  }

  @Override
  public void onSuccess(Response response) {
    instrumenter.end(context, request, response, null);
    try (Scope ignored = parentContext.makeCurrent()) {
      listener.onSuccess(response);
    }
  }

  @Override
  public void onFailure(Exception e) {
    instrumenter.end(context, request, null, e);
    try (Scope ignored = parentContext.makeCurrent()) {
      listener.onFailure(e);
    }
  }
}
