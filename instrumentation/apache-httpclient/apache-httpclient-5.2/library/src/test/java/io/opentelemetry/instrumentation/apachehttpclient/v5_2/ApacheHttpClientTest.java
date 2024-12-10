/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.apachehttpclient.v5_2;

import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.http.AbstractHttpClientTest;
import io.opentelemetry.instrumentation.testing.junit.http.HttpClientInstrumentationExtension;
import java.util.Collections;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;

class ApacheHttpClientTest extends AbstractApacheHttpClientTest {

  @RegisterExtension
  static final InstrumentationExtension testing = HttpClientInstrumentationExtension.forLibrary();

  @Override
  protected InstrumentationExtension testing() {
    return testing;
  }

  @Override
  protected CloseableHttpClient createClient(boolean readTimeout) {
    HttpClientBuilder builder =
        ApacheHttpClientTelemetry.builder(testing.getOpenTelemetry())
            .setCapturedRequestHeaders(
                Collections.singletonList(AbstractHttpClientTest.TEST_REQUEST_HEADER))
            .setCapturedResponseHeaders(
                Collections.singletonList(AbstractHttpClientTest.TEST_RESPONSE_HEADER))
            .build()
            .newHttpClientBuilder();
    builder.setDefaultRequestConfig(RequestConfig.custom().setMaxRedirects(2).build());

    ConnectionConfig.Builder connectionConfigBuilder =
        ConnectionConfig.custom()
            .setConnectTimeout(
                Timeout.ofMilliseconds(AbstractHttpClientTest.CONNECTION_TIMEOUT.toMillis()));

    if (readTimeout) {
      connectionConfigBuilder.setSocketTimeout(
          Timeout.ofMilliseconds(AbstractHttpClientTest.READ_TIMEOUT.toMillis()));
    }

    PoolingHttpClientConnectionManager connManager =
        PoolingHttpClientConnectionManagerBuilder.create()
            .setDefaultConnectionConfig(connectionConfigBuilder.build())
            .build();

    builder.setConnectionManager(connManager);
    builder.setConnectionManagerShared(true);
    return builder.build();
  }
}
