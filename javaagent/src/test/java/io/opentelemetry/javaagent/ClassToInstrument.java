/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent;

import io.opentracing.contrib.dropwizard.Trace;

@SuppressWarnings("PrivateConstructorForUtilityClass")
public class ClassToInstrument {

  @Trace
  public static void someMethod() {}
}
