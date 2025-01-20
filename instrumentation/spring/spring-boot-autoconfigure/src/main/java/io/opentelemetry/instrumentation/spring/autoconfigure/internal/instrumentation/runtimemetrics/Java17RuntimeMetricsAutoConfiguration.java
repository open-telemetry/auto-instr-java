/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.spring.autoconfigure.internal.instrumentation.runtimemetrics;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.instrumentation.runtimemetrics.java17.RuntimeMetrics;
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
@ConditionalOnEnabledInstrumentation(module = "runtime-telemetry-java17")
@Configuration
public class Java17RuntimeMetricsAutoConfiguration {

  private static final Logger logger =
      LoggerFactory.getLogger(Java17RuntimeMetricsAutoConfiguration.class);

  @EventListener
  public void handleApplicationReadyEvent(ApplicationReadyEvent event) {
    if (Double.parseDouble(System.getProperty("java.specification.version")) < 17) {
      logger.debug(
          "Java 17 runtime metrics instrumentation enabled but running on Java version < 17");
      return;
    }
    logger.debug(
        "Java 17 runtime metrics instrumentation enabled and running on Java version >= 17");
    ConfigurableApplicationContext applicationContext = event.getApplicationContext();
    OpenTelemetry openTelemetry = applicationContext.getBean(OpenTelemetry.class);
    ConfigProperties configProperties = applicationContext.getBean(ConfigProperties.class);
    RuntimeMetrics.builder(openTelemetry)
        .startFromInstrumentationConfig(new ConfigPropertiesBridge(configProperties));
  }
}
