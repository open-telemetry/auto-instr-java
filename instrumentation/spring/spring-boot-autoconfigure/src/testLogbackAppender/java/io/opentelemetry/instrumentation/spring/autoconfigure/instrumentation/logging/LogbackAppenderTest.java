/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.spring.autoconfigure.instrumentation.logging;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import io.opentelemetry.instrumentation.testing.internal.AutoCleanupExtension;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.LibraryInstrumentationExtension;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

class LogbackAppenderTest {

  @RegisterExtension
  static final InstrumentationExtension testing = LibraryInstrumentationExtension.create();

  @RegisterExtension static final AutoCleanupExtension cleanup = AutoCleanupExtension.create();

  @BeforeEach
  void setUp() {
    // reset the appender
    OpenTelemetryAppender.install(null);
  }

  @Configuration
  static class TestingOpenTelemetryConfiguration {

    @Bean
    public OpenTelemetry openTelemetry() {
      return testing.getOpenTelemetry();
    }
  }

  @Test
  void shouldInitializeAppender() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("logging.config", "classpath:logback-test.xml");
    properties.put(
        "otel.instrumentation.logback-appender.experimental.capture-mdc-attributes", "*");
    properties.put(
        "otel.instrumentation.logback-appender.experimental.capture-code-attributes", false);

    SpringApplication app =
        new SpringApplication(
            TestingOpenTelemetryConfiguration.class, OpenTelemetryAppenderAutoConfiguration.class);
    app.setDefaultProperties(properties);
    ConfigurableApplicationContext context = app.run();
    cleanup.deferCleanup(context);

    MDC.put("key1", "val1");
    MDC.put("key2", "val2");
    try {
      LoggerFactory.getLogger("test").info("test log message");
    } finally {
      MDC.clear();
    }

    List<LogRecordData> logRecords = testing.logRecords();
    assertThat(logRecords)
        .satisfiesOnlyOnce(
            // OTel appender automatically added or from an XML file, it should not
            // be added a second time by LogbackAppenderApplicationListener
            logRecord -> {
              assertThat(logRecord.getInstrumentationScopeInfo().getName()).isEqualTo("test");
              assertThat(logRecord.getBody().asString()).contains("test log message");

              Attributes attributes = logRecord.getAttributes();
              // key1 and key2, the code attributes should not be present because they are enabled
              // in the logback.xml file but are disabled with a property
              assertThat(attributes.size()).isEqualTo(2);
              assertThat(attributes.asMap())
                  .containsEntry(AttributeKey.stringKey("key1"), "val1")
                  .containsEntry(AttributeKey.stringKey("key2"), "val2");
            });
  }

  @Test
  void shouldNotInitializeAppenderWhenDisabled() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("logging.config", "classpath:logback-test.xml");
    properties.put("otel.springboot.logback-appender.enabled", "false");

    SpringApplication app =
        new SpringApplication(
            TestingOpenTelemetryConfiguration.class, OpenTelemetryAppenderAutoConfiguration.class);
    app.setDefaultProperties(properties);
    ConfigurableApplicationContext context = app.run();
    cleanup.deferCleanup(context);

    LoggerFactory.getLogger("test").info("test log message");

    assertThat(testing.logRecords()).isEmpty();
  }

  @Test
  void shouldNotInitializeAppenderWhenSdkDisabled() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("logging.config", "classpath:logback-test.xml");
    properties.put("otel.sdk.disabled", "true");

    SpringApplication app =
        new SpringApplication(
            TestingOpenTelemetryConfiguration.class, OpenTelemetryAppenderAutoConfiguration.class);
    app.setDefaultProperties(properties);
    ConfigurableApplicationContext context = app.run();
    cleanup.deferCleanup(context);

    LoggerFactory.getLogger("test").info("test log message");

    assertThat(testing.logRecords()).isEmpty();
  }

  @Test
  void shouldNotInitializeAppenderWhenInstrumentationDisabled() {
    Map<String, Object> properties = new HashMap<>();
    properties.put("logging.config", "classpath:logback-test.xml");
    properties.put("otel.instrumentation.logback-appender.enabled", "false");

    SpringApplication app =
        new SpringApplication(
            TestingOpenTelemetryConfiguration.class, OpenTelemetryAppenderAutoConfiguration.class);
    app.setDefaultProperties(properties);
    ConfigurableApplicationContext context = app.run();
    cleanup.deferCleanup(context);

    LoggerFactory.getLogger("test").info("test log message");

    assertThat(testing.logRecords()).isEmpty();
  }
}
