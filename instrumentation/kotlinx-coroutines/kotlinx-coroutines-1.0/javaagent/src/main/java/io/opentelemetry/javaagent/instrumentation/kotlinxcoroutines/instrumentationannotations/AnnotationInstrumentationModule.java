/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.kotlinxcoroutines.instrumentationannotations;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static java.util.Collections.singletonList;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.internal.ExperimentalInstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.internal.injection.ClassInjector;
import io.opentelemetry.javaagent.extension.instrumentation.internal.injection.InjectionMode;
import java.util.List;
import net.bytebuddy.matcher.ElementMatcher;

/** Instrumentation for methods annotated with {@code WithSpan} annotation. */
@AutoService(InstrumentationModule.class)
public class AnnotationInstrumentationModule extends InstrumentationModule
    implements ExperimentalInstrumentationModule {

  public AnnotationInstrumentationModule() {
    super(
        "kotlinx-coroutines",
        "kotlinx-coroutines-1.0",
        "kotlinx-coroutines-opentelemetry-instrumentation-annotations",
        "opentelemetry-instrumentation-annotations");
  }

  @Override
  public int order() {
    // Run first to ensure other automatic instrumentation is added after and therefore is executed
    // earlier in the instrumented method and create the span to attach attributes to.
    return -1000;
  }

  @Override
  public ElementMatcher.Junction<ClassLoader> classLoaderMatcher() {
    return hasClassesNamed(
        "application.io.opentelemetry.instrumentation.annotations.WithSpan",
        "kotlinx.coroutines.CoroutineContextKt");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return singletonList(new WithSpanInstrumentation());
  }

  @Override
  public void injectClasses(ClassInjector injector) {
    // AnnotationInstrumentationHelper is called directly in the instrumented bytecode.
    //
    // With invokedynamic instrumentation a proxy class can be used as long as it does not pull
    // extra types in the method signatures (which would require those types to also be available
    // in the instrumented code).
    injector
        .proxyBuilder(
            "io.opentelemetry.javaagent.instrumentation.kotlinxcoroutines.instrumentationannotations.AnnotationInstrumentationHelper")
        .inject(InjectionMode.CLASS_ONLY);
  }
}
