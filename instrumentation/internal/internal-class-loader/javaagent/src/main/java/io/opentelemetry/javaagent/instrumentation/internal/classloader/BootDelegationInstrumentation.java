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
import static net.bytebuddy.matcher.ElementMatchers.namedOneOf;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.opentelemetry.javaagent.bootstrap.CallDepth;
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
 * Some class loaders do not delegate to their parent, so classes in those class loaders will not be
 * able to see classes in the bootstrap class loader.
 *
 * <p>In particular, instrumentation on classes in those class loaders will not be able to see the
 * shaded OpenTelemetry API classes in the bootstrap class loader.
 *
 * <p>This instrumentation forces all class loaders to delegate to the bootstrap class loader for
 * the classes that we have put in the bootstrap class loader.
 */
public class BootDelegationInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    // just an optimization to exclude common class loaders that are known to delegate to the
    // bootstrap loader (or happen to _be_ the bootstrap loader)
    return not(namedOneOf("java.lang.ClassLoader", "com.ibm.oti.vm.BootstrapClassLoader"))
        .and(extendsClass(named("java.lang.ClassLoader")));
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
                methodMatcher, BootDelegationInstrumentation.class.getName() + "$LoadClassAdvice"));
  }

  @SuppressWarnings("unused")
  public static class LoadClassAdvice {

    @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
    public static Class<?> onEnter(@Advice.Argument(0) String name) {
      // need to use call depth here to prevent re-entry from call to Class.forName() below
      // because on some JVMs (e.g. IBM's, though IBM bootstrap loader is explicitly excluded above)
      // Class.forName() ends up calling loadClass() on the bootstrap loader which would then come
      // back to this instrumentation over and over, causing a StackOverflowError
      CallDepth callDepth = CallDepth.forClass(ClassLoader.class);
      if (callDepth.getAndIncrement() > 0) {
        callDepth.decrementAndGet();
        return null;
      }

      try {
        for (String prefix : BootstrapPackagesHelper.bootstrapPackagesPrefixes) {
          if (name.startsWith(prefix)) {
            try {
              return Class.forName(name, false, null);
            } catch (ClassNotFoundException ignored) {
              // Ignore
            }
          }
        }
      } finally {
        // need to reset it right away, not waiting until onExit()
        // otherwise it will prevent this instrumentation from being applied when loadClass()
        // ends up calling a ClassFileTransformer which ends up calling loadClass() further down the
        // stack on one of our bootstrap packages (since the call depth check would then suppress
        // the nested loadClass instrumentation)
        callDepth.decrementAndGet();
      }
      return null;
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void onExit(
        @Advice.Return(readOnly = false) Class<?> result,
        @Advice.Enter Class<?> resultFromBootstrapLoader) {
      if (resultFromBootstrapLoader != null) {
        result = resultFromBootstrapLoader;
      }
    }
  }
}
