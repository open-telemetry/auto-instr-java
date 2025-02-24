/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.annotations.incubator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation allows for adding attributes to the metrics recorded using {@link Timed} and
 * {@link Counted} annotations.
 *
 * <p>Application developers can use this annotation to signal OpenTelemetry auto-instrumentation
 * that the attribute should be created.
 *
 * <p>If you are a library developer, then probably you should NOT use this annotation, because it
 * is non-functional without the OpenTelemetry auto-instrumentation agent, or some other annotation
 * processor.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface StaticMetricAttributes {

  /** Array of {@link StaticMetricAttribute} annotations describing the added attributes. */
  StaticMetricAttribute[] value();
}
