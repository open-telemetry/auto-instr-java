/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.v4_0.commons;

import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.util.VirtualField;
import io.opentelemetry.javaagent.instrumentation.apachehttpclient.commons.OtelHttpInternalEntityStorage;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;

public final class ApacheHttpClientInernalEntityStorage
    extends OtelHttpInternalEntityStorage<HttpRequest, HttpResponse> {
  private static final ApacheHttpClientInernalEntityStorage INSTANCE;

  static {
    INSTANCE = new ApacheHttpClientInernalEntityStorage();
  }

  private ApacheHttpClientInernalEntityStorage() {
    super(
        VirtualField.find(Context.class, HttpRequest.class),
        VirtualField.find(Context.class, HttpResponse.class));
  }

  public static OtelHttpInternalEntityStorage<HttpRequest, HttpResponse> storage() {
    return INSTANCE;
  }
}
