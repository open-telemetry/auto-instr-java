/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.junit.v5_0;

import org.junit.jupiter.api.extension.ExtensionContext.Namespace;

public class JunitConstants {
  public static final Namespace OPENTELEMETRY_TRACING_EXTENSION_NAMESPACE =
      Namespace.create(
          "io",
          "opentelemetry",
          "instrumentation",
          "junit",
          "v5_0",
          "OpenTelemetryTracingExtension");

  public static final String JUNIT_TEST_LIFECYCLE = "junit.test.lifecycle";
  public static final String JUNIT_TEST_ARGUMENTS = "junit.test.arguments";
  public static final String JUNIT_TEST_RESULT = "junit.test.result";
  public static final String JUNIT_OPENTELEMETRY_ROOT_CONTEXT = "junit.opentelemetry.root.context";
  public static final String JUNIT_OPENTELEMETRY_ROOT_SCOPE = "junit.opentelemetry.root.scope";
  public static final String JUNIT_OPENTELEMETRY_TEST_CONTEXT = "junit.opentelemetry.test.context";
  public static final String JUNIT_OPENTELEMETRY_TEST_SCOPE = "junit.opentelemetry.test.scope";
  public static final String JUNIT_SPAN_NAME = "junit.opentelemetry.span.name";

  private JunitConstants() {}
}
