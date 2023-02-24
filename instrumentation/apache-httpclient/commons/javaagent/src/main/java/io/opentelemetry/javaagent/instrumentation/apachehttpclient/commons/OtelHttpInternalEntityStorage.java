/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.commons;

import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.util.VirtualField;

public abstract class OtelHttpInternalEntityStorage<REQ, RES> {
  private final VirtualField<Context, REQ> requestStorage;
  private final VirtualField<Context, RES> responseStorage;

  protected OtelHttpInternalEntityStorage(
      VirtualField<Context, REQ> requestStorage, VirtualField<Context, RES> responseStorage) {
    this.requestStorage = requestStorage;
    this.responseStorage = responseStorage;
  }

  // http client internally makes a copy of the user request, we are storing it
  // from the interceptor, to be able to fetch actually sent headers by the client
  public void storeHttpRequest(Context context, REQ httpRequest) {
    if (httpRequest != null) {
      requestStorage.set(context, httpRequest);
    }
  }

  // in cases of failures (like circular redirects), callbacks may not receive the actual response
  // from the client, hence we are storing this response from interceptor to fetch attributes
  public void storeHttpResponse(Context context, RES httpResponse) {
    if (httpResponse != null) {
      responseStorage.set(context, httpResponse);
    }
  }

  public REQ getInternalRequest(Context context) {
    return requestStorage.get(context);
  }

  public RES getInternalResponse(Context context) {
    return responseStorage.get(context);
  }
}
