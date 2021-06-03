/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.jetty.httpclient.v9_0


import io.opentelemetry.instrumentation.test.AgentTestTrait
import org.eclipse.jetty.client.HttpClient
import org.eclipse.jetty.util.ssl.SslContextFactory

class JettyHttpClient9AgentTest extends AbstractJettyClient9Test implements AgentTestTrait {
  
  @Override
  HttpClient createStandardClient() {
    return new HttpClient()
  }

  @Override
  HttpClient createHttpsClient(SslContextFactory sslContextFactory) {
    return new HttpClient(sslContextFactory)
  }
}
