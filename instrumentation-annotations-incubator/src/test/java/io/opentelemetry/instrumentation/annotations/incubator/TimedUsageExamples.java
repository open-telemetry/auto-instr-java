/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.annotations.incubator;

public class TimedUsageExamples {

  @Timed("customizedName")
  public void method() {}

  @Timed("methodWithAttributes")
  public void attributes(
      @MetricAttribute String attribute1, @MetricAttribute("attribute2") long attribute2) {}
}
