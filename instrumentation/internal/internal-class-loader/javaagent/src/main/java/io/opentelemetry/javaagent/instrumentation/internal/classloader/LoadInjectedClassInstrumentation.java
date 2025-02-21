/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.internal.classloader;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.extendsClass;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isProtected;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.isStatic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.opentelemetry.javaagent.bootstrap.InjectedClassHelper;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import io.opentelemetry.javaagent.tooling.Utils;
import io.opentelemetry.javaagent.tooling.bytebuddy.ExceptionHandlers;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

/**
 * This instrumentation inserts loading of our injected helper classes at the start of {@code
 * ClassLoader.loadClass} method.
 */
public class LoadInjectedClassInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return extendsClass(named("java.lang.ClassLoader"));
  }

  @Override
  public void transform(TypeTransformer transformer) {
    ElementMatcher.Junction<MethodDescription> methodMatcher =
        isMethod()
            .and(named("loadClass"))
            .and(
                takesArguments(1)
                    .and(takesArgument(0, String.class))
                    .or(
                        takesArguments(2)
                            .and(takesArgument(0, String.class))
                            .and(takesArgument(1, boolean.class))))
            .and(isPublic().or(isProtected()))
            .and(not(isStatic()));
    // Inline instrumentation to prevent problems with invokedynamic-recursion
    transformer.applyTransformer(
        new AgentBuilder.Transformer.ForAdvice()
            .include(Utils.getBootstrapProxy(), Utils.getAgentClassLoader())
            .withExceptionHandler(ExceptionHandlers.defaultExceptionHandler())
            .advice(
                methodMatcher,
                LoadInjectedClassInstrumentation.class.getName() + "$LoadClassAdvice"));
  }

  @SuppressWarnings("unused")
  public static class LoadClassAdvice {

    @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
    public static Class<?> onEnter(
        @Advice.This ClassLoader classLoader, @Advice.Argument(0) String name) {
      Class<?> helperClass = InjectedClassHelper.loadHelperClass(classLoader, name);
      if (helperClass != null) {
        return helperClass;
      }

      return null;
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class)
    public static void onExit(
        @Advice.Return(readOnly = false) Class<?> result, @Advice.Enter Class<?> loadedClass) {
      if (loadedClass != null) {
        result = loadedClass;
      }
    }
  }
}
