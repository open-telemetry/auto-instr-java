/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.spring.smoketest;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.metrics.LongHistogram;
import io.opentelemetry.api.metrics.Meter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class OtelSpringStarterSmokeTestController {

  public static final String PING = "/ping";
  public static final String TEST_HISTOGRAM = "histogram-test-otel-spring-starter";
  public static final String METER_SCOPE_NAME = "scope";
  private final LongHistogram histogram;
  private final SpringComponent component;

  public OtelSpringStarterSmokeTestController(
      OpenTelemetry openTelemetry, SpringComponent springComponent) {
    Meter meter = openTelemetry.getMeter(METER_SCOPE_NAME);
    histogram = meter.histogramBuilder(TEST_HISTOGRAM).ofLongs().build();
    this.component = springComponent;
  }

  @GetMapping(PING)
  public String ping() {
    histogram.record(10);
    component.withSpanMethod("from-controller");
    return "pong";
  }
}
