/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.annotations.incubator;

public class CountedUsageExamples {

  @Counted("customizedName")
  public void method() {}

  @Counted("methodWithAttributes")
  public void attributes(
      @MetricAttribute String attribute1, @MetricAttribute("attribute2") long attribute2) {}
}
