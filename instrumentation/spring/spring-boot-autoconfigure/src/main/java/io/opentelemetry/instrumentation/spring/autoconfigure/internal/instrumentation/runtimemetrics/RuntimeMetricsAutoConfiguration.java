/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.spring.autoconfigure.internal.instrumentation.runtimemetrics;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.spring.autoconfigure.internal.ConditionalOnEnabledInstrumentation;
import io.opentelemetry.instrumentation.spring.autoconfigure.internal.properties.ConfigPropertiesBridge;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;

/**
 * Configures runtime metrics collection.
 *
 * <p>This class is internal and is hence not for public use. Its APIs are unstable and can change
 * at any time.
 */
@ConditionalOnEnabledInstrumentation(module = "runtime-telemetry")
@Configuration
public class RuntimeMetricsAutoConfiguration {

  private static final Logger logger =
      LoggerFactory.getLogger(RuntimeMetricsAutoConfiguration.class);

  @EventListener
  public void handleApplicationReadyEvent(ApplicationReadyEvent event) {
    ConfigurableApplicationContext applicationContext = event.getApplicationContext();
    OpenTelemetry openTelemetry = applicationContext.getBean(OpenTelemetry.class);
    ConfigPropertiesBridge config =
        new ConfigPropertiesBridge(applicationContext.getBean(ConfigProperties.class));

    if (Double.parseDouble(System.getProperty("java.specification.version")) >= 17) {
      logger.debug("Use runtime metrics instrumentation for Java 17+");
      io.opentelemetry.instrumentation.runtimemetrics.java17.RuntimeMetrics.builder(openTelemetry)
          .startFromInstrumentationConfig(config);
    } else {
      logger.debug("Use runtime metrics instrumentation for Java 8");
      io.opentelemetry.instrumentation.runtimemetrics.java8.RuntimeMetrics.builder(openTelemetry)
          .startFromInstrumentationConfig(config);
    }
  }
}
