/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.spring.actuator.v2_0;

import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.assertThat;
import static io.opentelemetry.sdk.testing.assertj.OpenTelemetryAssertions.equalTo;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.instrumentation.testing.internal.AutoCleanupExtension;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import io.opentelemetry.instrumentation.testing.junit.InstrumentationExtension;
import io.opentelemetry.javaagent.instrumentation.spring.actuator.v2_0.SpringApp.TestBean;
import java.util.Collection;
import org.assertj.core.api.AbstractCollectionAssert;
import org.assertj.core.api.ObjectAssert;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

class ActuatorTest {

  @RegisterExtension
  static final InstrumentationExtension testing = AgentInstrumentationExtension.create();

  @RegisterExtension static final AutoCleanupExtension cleanup = AutoCleanupExtension.create();

  @Test
  void shouldInjectOtelMeterRegistry() {
    SpringApplication app = new SpringApplication(SpringApp.class);
    ConfigurableApplicationContext context = app.run();
    cleanup.deferCleanup(context);

    TestBean testBean = context.getBean(TestBean.class);
    testBean.inc();

    testing.waitAndAssertMetrics(
        "io.opentelemetry.micrometer-1.5",
        "test-counter",
        metrics ->
            metrics.anySatisfy(
                metric ->
                    assertThat(metric)
                        .hasUnit("thingies")
                        .hasDoubleSumSatisfying(
                            sum ->
                                sum.isMonotonic()
                                    .hasPointsSatisfying(
                                        point ->
                                            point
                                                .hasValue(1)
                                                .hasAttributesSatisfying(
                                                    equalTo(
                                                        AttributeKey.stringKey("tag"),
                                                        "value"))))));

    MeterRegistry meterRegistry = context.getBean(MeterRegistry.class);
    assertThat(meterRegistry).isInstanceOf(CompositeMeterRegistry.class);
    AbstractCollectionAssert<
            ?, Collection<? extends MeterRegistry>, MeterRegistry, ObjectAssert<MeterRegistry>>
        match =
            assertThat(((CompositeMeterRegistry) meterRegistry).getRegistries())
                .anyMatch(r -> r.getClass().getSimpleName().equals("OpenTelemetryMeterRegistry"))
                .anyMatch(r -> r.getClass().getSimpleName().equals("SimpleMeterRegistry"));

    try {
      Class.forName("io.micrometer.prometheusmetrics.PrometheusMeterRegistry");
      match.anyMatch(r -> r.getClass().getSimpleName().equals("PrometheusMeterRegistry"));
    } catch (ClassNotFoundException e) {
      // not testing prometheus
    }
  }
}
