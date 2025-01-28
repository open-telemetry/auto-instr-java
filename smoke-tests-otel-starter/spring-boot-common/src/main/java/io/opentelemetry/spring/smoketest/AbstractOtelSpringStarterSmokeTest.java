/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.spring.smoketest;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.satisfies;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.instrumentation.spring.autoconfigure.internal.properties.OtelResourceProperties;
import io.opentelemetry.instrumentation.spring.autoconfigure.internal.properties.OtelSpringProperties;
import io.opentelemetry.instrumentation.spring.autoconfigure.internal.properties.OtlpExporterProperties;
import io.opentelemetry.instrumentation.spring.autoconfigure.internal.properties.SpringConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import io.opentelemetry.sdk.autoconfigure.spi.internal.DefaultConfigProperties;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.resources.Resource;
import io.opentelemetry.semconv.ClientAttributes;
import io.opentelemetry.semconv.HttpAttributes;
import io.opentelemetry.semconv.ServerAttributes;
import io.opentelemetry.semconv.UrlAttributes;
import io.opentelemetry.semconv.incubating.CodeIncubatingAttributes;
import io.opentelemetry.semconv.incubating.DbIncubatingAttributes;
import io.opentelemetry.semconv.incubating.ServiceIncubatingAttributes;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import org.assertj.core.api.AbstractCharSequenceAssert;
import org.assertj.core.api.AbstractIterableAssert;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.client.RestTemplate;

/**
 * This test class enforces the order of the tests to make sure that {@link #shouldSendTelemetry()},
 * which asserts the telemetry data from the application startup, is executed first.
 */
@SuppressWarnings("deprecation") // using deprecated semconv
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AbstractOtelSpringStarterSmokeTest extends AbstractSpringStarterSmokeTest {

  @Autowired private TestRestTemplate testRestTemplate;

  @Autowired private Environment environment;
  @Autowired private OtelSpringProperties otelSpringProperties;
  @Autowired private OtelResourceProperties otelResourceProperties;
  @Autowired private OtlpExporterProperties otlpExporterProperties;
  @Autowired private RestTemplateBuilder restTemplateBuilder;
  @Autowired private JdbcTemplate jdbcTemplate;

  // can't use @LocalServerPort annotation since it moved packages between Spring Boot 2 and 3
  @Value("${local.server.port}")
  private int port;

  @Configuration(proxyBeanMethods = false)
  static class TestConfiguration {
    @Autowired private ObjectProvider<JdbcTemplate> jdbcTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void loadData() {
      jdbcTemplate
          .getObject()
          .execute(
              "create table customer (id bigint not null, name varchar not null, primary key (id))");
    }

    @Bean
    @Order(1)
    AutoConfigurationCustomizerProvider hiddenPropagatorCustomizer() {
      return customizer ->
          customizer.addResourceCustomizer(
              (resource, config) ->
                  resource.merge(
                      Resource.create(
                          Attributes.of(
                              AttributeKey.booleanKey("keyFromResourceCustomizer"), false))));
    }

    @Bean
    @Order(2)
    AutoConfigurationCustomizerProvider propagatorCustomizer() {
      return customizer ->
          customizer.addResourceCustomizer(
              (resource, config) ->
                  resource.merge(
                      Resource.create(
                          Attributes.of(
                              AttributeKey.booleanKey("keyFromResourceCustomizer"), true))));
    }

    @Bean
    AutoConfigurationCustomizerProvider customizerUsingPropertyDefinedInaSpringFile() {
      return customizer ->
          customizer.addResourceCustomizer(
              (resource, config) -> {
                String valueForKeyDeclaredZsEnvVariable = config.getString("APPLICATION_PROP");
                assertThat(valueForKeyDeclaredZsEnvVariable).isNotEmpty();

                String valueForKeyWithDash = config.getString("application.prop-with-dash");
                assertThat(valueForKeyWithDash).isNotEmpty();

                return resource;
              });
    }
  }

  @Test
  void propertyConversion() {
    ConfigProperties configProperties =
        SpringConfigProperties.create(
            environment,
            otlpExporterProperties,
            otelResourceProperties,
            otelSpringProperties,
            DefaultConfigProperties.createFromMap(
                Collections.singletonMap("otel.exporter.otlp.headers", "a=1,b=2")));
    assertThat(configProperties.getMap("otel.exporter.otlp.headers"))
        .containsEntry("a", "1")
        .containsEntry("b", "2")
        .containsEntry("c", "3");
    assertThat(configProperties.getList("otel.propagators")).containsExactly("b3");
  }

  @Test
  @org.junit.jupiter.api.Order(1)
  void shouldSendTelemetry() {
    HttpHeaders headers = new HttpHeaders();
    headers.add("key", "value");

    testRestTemplate.exchange(
        new RequestEntity<>(
            null, headers, HttpMethod.GET, URI.create(OtelSpringStarterSmokeTestController.PING)),
        String.class);

    // Span
    testing.waitAndAssertTraces(
        traceAssert ->
            traceAssert.hasSpansSatisfyingExactly(
                spanDataAssert ->
                    spanDataAssert
                        .hasKind(SpanKind.CLIENT)
                        .hasAttribute(
                            DbIncubatingAttributes.DB_STATEMENT,
                            "create table customer (id bigint not null, name varchar not null, primary key (id))")),
        traceAssert ->
            traceAssert.hasSpansSatisfyingExactly(
                clientSpan ->
                    clientSpan
                        .hasKind(SpanKind.CLIENT)
                        .hasAttributesSatisfying(
                            satisfies(
                                UrlAttributes.URL_FULL,
                                stringAssert -> stringAssert.endsWith("/ping")),
                            equalTo(ServerAttributes.SERVER_ADDRESS, "localhost"),
                            satisfies(
                                ServerAttributes.SERVER_PORT,
                                integerAssert -> integerAssert.isNotZero())),
                serverSpan ->
                    HttpSpanDataAssert.create(serverSpan)
                        .assertServerGetRequest("/ping")
                        .hasResourceSatisfying(
                            r ->
                                r.hasAttribute(
                                        AttributeKey.booleanKey("keyFromResourceCustomizer"), true)
                                    .hasAttribute(
                                        AttributeKey.stringKey("attributeFromYaml"), "true")
                                    .hasAttribute(
                                        satisfies(
                                            ServiceIncubatingAttributes.SERVICE_INSTANCE_ID,
                                            AbstractCharSequenceAssert::isNotBlank)))
                        .hasAttributesSatisfying(
                            equalTo(HttpAttributes.HTTP_REQUEST_METHOD, "GET"),
                            equalTo(HttpAttributes.HTTP_RESPONSE_STATUS_CODE, 200L),
                            equalTo(HttpAttributes.HTTP_ROUTE, "/ping"),
                            equalTo(ServerAttributes.SERVER_ADDRESS, "localhost"),
                            equalTo(ClientAttributes.CLIENT_ADDRESS, "127.0.0.1"),
                            equalTo(
                                AttributeKey.stringArrayKey("http.request.header.key"),
                                Collections.singletonList("value")),
                            satisfies(
                                ServerAttributes.SERVER_PORT,
                                integerAssert -> integerAssert.isNotZero())),
                span -> withSpanAssert(span)));

    // Metric
    testing.waitAndAssertMetrics(
        OtelSpringStarterSmokeTestController.METER_SCOPE_NAME,
        OtelSpringStarterSmokeTestController.TEST_HISTOGRAM,
        AbstractIterableAssert::isNotEmpty);
    // runtime metrics
    testing.waitAndAssertMetrics(
        "io.opentelemetry.runtime-telemetry-java8",
        "jvm.thread.count",
        AbstractIterableAssert::isNotEmpty);

    // Log
    List<LogRecordData> exportedLogRecords = testing.getExportedLogRecords();
    assertThat(exportedLogRecords).as("No log record exported.").isNotEmpty();
    if (System.getProperty("org.graalvm.nativeimage.imagecode") == null) {
      // log records differ in native image mode due to different startup timing
      LogRecordData firstLog = exportedLogRecords.get(0);
      assertThat(firstLog.getBodyValue().asString())
          .as("Should instrument logs")
          .startsWith("Starting ")
          .contains(this.getClass().getSimpleName());
      assertThat(firstLog.getAttributes().asMap())
          .as("Should capture code attributes")
          .containsEntry(
              CodeIncubatingAttributes.CODE_NAMESPACE,
              "org.springframework.boot.StartupInfoLogger");
    }
  }

  @Test
  void databaseQuery() {
    testing.clearAllExportedData();

    testing.runWithSpan(
        "server",
        () -> {
          jdbcTemplate.query(
              "select name from customer where id = 1", (rs, rowNum) -> rs.getString("name"));
        });

    // 1 is not replaced by ?, otel.instrumentation.common.db-statement-sanitizer.enabled=false
    testing.waitAndAssertTraces(
        traceAssert ->
            traceAssert.hasSpansSatisfyingExactly(
                span -> span.hasName("server"),
                span -> span.satisfies(s -> assertThat(s.getName()).endsWith(".getConnection")),
                span ->
                    span.hasKind(SpanKind.CLIENT)
                        .hasAttribute(
                            DbIncubatingAttributes.DB_STATEMENT,
                            "select name from customer where id = 1")));
  }

  @Test
  void restTemplate() {
    testing.clearAllExportedData();

    RestTemplate restTemplate = restTemplateBuilder.rootUri("http://localhost:" + port).build();
    restTemplate.getForObject(OtelSpringStarterSmokeTestController.PING, String.class);
    testing.waitAndAssertTraces(
        traceAssert ->
            traceAssert.hasSpansSatisfyingExactly(
                span -> HttpSpanDataAssert.create(span).assertClientGetRequest("/ping"),
                span ->
                    span.hasKind(SpanKind.SERVER).hasAttribute(HttpAttributes.HTTP_ROUTE, "/ping"),
                span -> withSpanAssert(span)));
  }
}
