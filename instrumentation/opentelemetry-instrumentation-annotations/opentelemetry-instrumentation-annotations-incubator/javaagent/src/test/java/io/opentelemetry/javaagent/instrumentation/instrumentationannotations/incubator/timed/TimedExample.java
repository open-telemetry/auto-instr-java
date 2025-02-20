/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.instrumentationannotations.incubator.timed;

import io.opentelemetry.instrumentation.annotations.incubator.MetricAttributeForReturnValue;
import io.opentelemetry.instrumentation.annotations.incubator.StaticMetricAttribute;
import io.opentelemetry.instrumentation.annotations.incubator.Timed;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class TimedExample {
  public static final String METRIC_NAME = "name.duration";
  public static final String METRIC_DESCRIPTION = "I am the description.";
  public static final String RETURN_STRING = "I am a return string.";

  @Timed(METRIC_NAME)
  public void exampleWithName() {}

  @Timed(value = "example.with.description.duration", description = METRIC_DESCRIPTION)
  public void exampleWithDescription() {}

  @Timed(value = "example.with.unit.duration", unit = TimeUnit.MILLISECONDS)
  public void exampleWithUnit() throws InterruptedException {
    Thread.sleep(2000);
  }

  @Timed("example.with.attributes.duration")
  @StaticMetricAttribute(name = "key1", value = "value1")
  @StaticMetricAttribute(name = "key2", value = "value2")
  public void exampleWithAdditionalAttributes1() {}

  @Timed("example.ignore.duration")
  public void exampleIgnore() {}

  @Timed("example.with.exception.duration")
  public void exampleWithException() {
    throw new IllegalStateException("test");
  }

  @Timed(value = "example.with.return.duration")
  @MetricAttributeForReturnValue("returnValue")
  public ReturnObject exampleWithReturnValueAttribute() {
    return new ReturnObject();
  }

  @Timed(value = "example.completable.future.duration")
  @MetricAttributeForReturnValue("returnValue")
  public CompletableFuture<String> completableFuture(CompletableFuture<String> future) {
    return future;
  }

  public static class ReturnObject {
    @Override
    public String toString() {
      return RETURN_STRING;
    }
  }
}
