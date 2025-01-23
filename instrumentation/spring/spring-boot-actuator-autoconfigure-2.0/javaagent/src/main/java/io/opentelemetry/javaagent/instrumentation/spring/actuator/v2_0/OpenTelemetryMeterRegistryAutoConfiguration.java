/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.spring.actuator.v2_0;

import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.javaagent.instrumentation.micrometer.v1_5.MicrometerSingletons;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.prometheus.PrometheusMetricsExportAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.export.simple.SimpleMetricsExportAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
// CompositeMeterRegistryAutoConfiguration configures the "final" composite registry
@AutoConfigureBefore(CompositeMeterRegistryAutoConfiguration.class)
// configure after the SimpleMeterRegistry has initialized; it is normally the last MeterRegistry
// implementation to be configured, as it's used as a fallback
// the OTel registry should be added in addition to that fallback and not replace it
@AutoConfigureAfter({
  SimpleMetricsExportAutoConfiguration.class,
  PrometheusMetricsExportAutoConfiguration.class
})
@ConditionalOnBean(Clock.class)
@ConditionalOnClass(MeterRegistry.class)
public class OpenTelemetryMeterRegistryAutoConfiguration {

  @Bean
  public MeterRegistry otelMeterRegistry() {
    return MicrometerSingletons.meterRegistry();
  }
}
