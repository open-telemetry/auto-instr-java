/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.instrumentationannotations.incubator.counted;

import static io.opentelemetry.javaagent.instrumentation.instrumentationannotations.incubator.counted.CountedExample.METRIC_DESCRIPTION;
import static io.opentelemetry.javaagent.instrumentation.instrumentationannotations.incubator.counted.CountedExample.METRIC_NAME;
import static io.opentelemetry.javaagent.instrumentation.instrumentationannotations.incubator.counted.CountedExample.METRIC_UNIT;
import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.instrumentation.testing.junit.AgentInstrumentationExtension;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

class CountedInstrumentationTest {

  @RegisterExtension
  private static final AgentInstrumentationExtension testing =
      AgentInstrumentationExtension.create();

  private static final String INSTRUMENTATION_NAME =
      "io.opentelemetry.opentelemetry-instrumentation-annotations-incubator";

  @Test
  void testExampleWithAnotherName() {
    new CountedExample().exampleWithName();
    testing.waitAndAssertMetrics(INSTRUMENTATION_NAME, metric -> metric.hasName(METRIC_NAME));
  }

  @Test
  void testExampleWithDescription() {
    new CountedExample().exampleWithDescription();
    testing.waitAndAssertMetrics(
        INSTRUMENTATION_NAME,
        metric ->
            metric.hasName("example.with.description.count").hasDescription(METRIC_DESCRIPTION));
  }

  @Test
  void testExampleWithUnit() {
    new CountedExample().exampleWithUnit();
    testing.waitAndAssertMetrics(
        INSTRUMENTATION_NAME,
        metric -> metric.hasName("example.with.unit.count").hasUnit(METRIC_UNIT));
  }

  @Test
  void testExampleWithAdditionalAttributes1() {
    new CountedExample().exampleWithAdditionalAttributes1();
    testing.waitAndAssertMetrics(
        INSTRUMENTATION_NAME,
        metric ->
            metric
                .hasName("example.with.attributes.count")
                .satisfies(
                    metricData ->
                        assertThat(metricData.getData().getPoints())
                            .allMatch(
                                p ->
                                    "value1"
                                            .equals(
                                                p.getAttributes()
                                                    .get(AttributeKey.stringKey("key1")))
                                        && "value2"
                                            .equals(
                                                p.getAttributes()
                                                    .get(AttributeKey.stringKey("key2"))))));
  }

  @Test
  void testExampleWithReturnAttribute() {
    new CountedExample().exampleWithReturnValueAttribute();
    testing.waitAndAssertMetrics(
        INSTRUMENTATION_NAME,
        metric ->
            metric
                .hasName("example.with.return.count")
                .satisfies(
                    metricData ->
                        assertThat(metricData.getData().getPoints())
                            .allMatch(
                                p ->
                                    CountedExample.RETURN_STRING.equals(
                                        p.getAttributes()
                                            .get(AttributeKey.stringKey("returnValue"))))));
  }

  @Test
  void testExampleWithException() {
    try {
      new CountedExample().exampleWithException();
    } catch (IllegalStateException e) {
      // noop
    }
    testing.waitAndAssertMetrics(
        INSTRUMENTATION_NAME,
        metric ->
            metric
                .hasName("example.with.exception.count")
                .satisfies(
                    metricData ->
                        assertThat(metricData.getData().getPoints())
                            .allMatch(
                                p ->
                                    IllegalStateException.class
                                        .getName()
                                        .equals(
                                            p.getAttributes()
                                                .get(AttributeKey.stringKey("error.type"))))));
  }

  @Test
  void testExampleIgnore() throws Exception {
    new CountedExample().exampleIgnore();
    Thread.sleep(500); // sleep a bit just to make sure no metric is captured
    assertThat(testing.metrics()).isEmpty();
  }

  @Test
  void testCompletableFuture() throws Exception {
    CompletableFuture<String> future = new CompletableFuture<>();
    new CountedExample().completableFuture(future);

    Thread.sleep(500); // sleep a bit just to make sure no metric is captured
    assertThat(testing.metrics()).isEmpty();

    future.complete("Done");

    testing.waitAndAssertMetrics(
        INSTRUMENTATION_NAME,
        metric ->
            metric
                .hasName("example.completable.future.count")
                .satisfies(
                    metricData ->
                        assertThat(metricData.getData().getPoints())
                            .allMatch(
                                p ->
                                    "Done"
                                        .equals(
                                            p.getAttributes()
                                                .get(AttributeKey.stringKey("returnValue"))))));
  }
}
