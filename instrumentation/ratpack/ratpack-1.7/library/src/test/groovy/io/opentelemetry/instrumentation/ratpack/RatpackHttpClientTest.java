/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.ratpack;

import io.opentelemetry.instrumentation.ratpack.client.RatpackHttpTracing;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.http.HttpClientInstrumentationExtension;
import org.junit.jupiter.api.extension.RegisterExtension;
import ratpack.func.Action;
import ratpack.http.client.HttpClient;
import ratpack.http.client.HttpClientSpec;

class RatpackHttpClientTest extends AbstractRatpackHttpClientSpec {

  @RegisterExtension
  static final InstrumentationExtension testing = HttpClientInstrumentationExtension.forLibrary();

  @Override
  protected HttpClient buildHttpClient() throws Exception {
    return RatpackHttpTracing.create(testing.getOpenTelemetry())
        .instrumentedHttpClient(HttpClient.of(Action.noop()));
  }

  @Override
  protected HttpClient buildHttpClient(Action<? super HttpClientSpec> action) throws Exception {
    return RatpackHttpTracing.create(testing.getOpenTelemetry())
        .instrumentedHttpClient(HttpClient.of(action));
  }
}
