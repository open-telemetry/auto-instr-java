/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.junit.v5_0;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.junit.jupiter.api.extension.ExtendWith;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@ExtendWith(OpenTelemetryTracingExtension.class)
public @interface JunitOpenTelemetryTracing {
  String enabled() default "true";

  String traceExporter() default "otlp";

  String metricsExporter() default "none";

  String logExporter() default "none";

  String otlpEndpoint() default "http://localhost:4317";
}
