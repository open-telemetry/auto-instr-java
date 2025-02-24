/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.instrumentationannotations;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static java.util.Arrays.asList;

import application.io.opentelemetry.instrumentation.annotations.AddingSpanAttributes;
import application.io.opentelemetry.instrumentation.annotations.WithSpan;
import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import java.util.List;
import net.bytebuddy.matcher.ElementMatcher;

/**
 * Instrumentation for methods annotated with {@link WithSpan} and {@link AddingSpanAttributes}
 * annotations.
 */
@AutoService(InstrumentationModule.class)
public class AnnotationInstrumentationModule extends InstrumentationModule {

  public AnnotationInstrumentationModule() {
    super(
        "opentelemetry-instrumentation-annotations",
        "opentelemetry-instrumentation-annotations-1.16",
        "annotations");
  }

  @Override
  public int order() {
    // Run first to ensure other automatic instrumentation is added after and therefore is executed
    // earlier in the instrumented method and create the span to attach attributes to.
    return -1000;
  }

  @Override
  public ElementMatcher.Junction<ClassLoader> classLoaderMatcher() {
    return hasClassesNamed("application.io.opentelemetry.instrumentation.annotations.WithSpan");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return asList(new WithSpanInstrumentation(), new AddingSpanAttributesInstrumentation());
  }
}
