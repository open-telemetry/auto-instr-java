/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.spring.smoketest;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.UrlAttributes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;

@SpringBootTest(
    classes = {
      OtelSpringStarterSmokeTestApplication.class,
      AbstractOtelSpringStarterSmokeTest.TestConfiguration.class,
      SpringSmokeOtelConfiguration.class
    },
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OtelSpringStarterSmokeTest extends AbstractSpringStarterSmokeTest {

  @Autowired RestClient.Builder restClientBuilder;
  @LocalServerPort private int port;

  @Test
  void restClient() {
    testing.clearAllExportedData();

    RestClient client = restClientBuilder.baseUrl("http://localhost:" + port).build();
    assertThat(
            client
                .get()
                .uri(OtelSpringStarterSmokeTestController.PING)
                .retrieve()
                .body(String.class))
        .isEqualTo("pong");
    testing.waitAndAssertTraces(
        traceAssert ->
            traceAssert.hasSpansSatisfyingExactly(
                nestedClientSpan ->
                    nestedClientSpan
                        .hasKind(SpanKind.CLIENT)
                        .hasAttributesSatisfying(
                            a -> assertThat(a.get(UrlAttributes.URL_FULL)).endsWith("/ping")),
                nestedServerSpan ->
                    nestedServerSpan
                        .hasKind(SpanKind.SERVER)
                        .hasAttribute(HttpAttributes.HTTP_ROUTE, "/ping")));
  }
}
