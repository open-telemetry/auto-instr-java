/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.tooling.instrumentation.indy;

import io.opentelemetry.javaagent.bootstrap.CallDepth;
import io.opentelemetry.javaagent.bootstrap.IndyBootstrapDispatcher;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import java.lang.invoke.CallSite;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.annotation.AnnotationDescription;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.utility.JavaConstant;

/**
 * We instruct Byte Buddy (via {@link Advice.WithCustomMapping#bootstrap(java.lang.reflect.Method)})
 * to dispatch {@linkplain Advice.OnMethodEnter#inline() non-inlined advices} via an invokedynamic
 * (indy) instruction. The target method is linked to a dynamically created instrumentation module
 * class loader that is specific to an instrumentation module and the class loader of the
 * instrumented method.
 *
 * <p>The first invocation of an {@code INVOKEDYNAMIC} causes the JVM to dynamically link a {@link
 * CallSite}. In this case, it will use the {@link #bootstrap} method to do that. This will also
 * create the {@link InstrumentationModuleClassLoader}.
 *
 * <pre>
 *
 *   Bootstrap CL ←──────────────────────────── Agent CL
 *       ↑ └───────── IndyBootstrapDispatcher ─ ↑ ──→ └────────────── {@link IndyBootstrap#bootstrap}
 *     Ext/Platform CL               ↑          │                        ╷
 *       ↑                           ╷          │                        ↓
 *     System CL                     ╷          │        {@link IndyModuleRegistry#getInstrumentationClassLoader(String, ClassLoader)}
 *       ↑                           ╷          │                        ╷
 *     Common               linking of CallSite │                        ╷
 *     ↑    ↑             (on first invocation) │                        ╷
 * WebApp1  WebApp2                  ╷          │                     creates
 *          ↑ - InstrumentedClass    ╷          │                        ╷
 *          │                ╷       ╷          │                        ╷
 *          │                INVOKEDYNAMIC      │                        ↓
 *          └────────────────┼──────────────────{@link InstrumentationModuleClassLoader}
 *                           └╶╶╶╶╶╶╶╶╶╶╶╶╶╶╶╶╶╶╶╶╶→├ AdviceClass
 *                                                  ├ AdviceHelper
 *                                                  └ {@link LookupExposer}
 *
 * Legend:
 *  ╶╶→ method calls
 *  ──→ class loader parent/child relationships
 * </pre>
 */
public class IndyBootstrap {

  private static final Logger logger = Logger.getLogger(IndyBootstrap.class.getName());

  private static final Method indyBootstrapMethod;

  private static final String BOOTSTRAP_KIND_ADVICE = "advice";
  private static final String BOOTSTRAP_KIND_PROXY = "proxy";

  private static final String PROXY_KIND_STATIC = "static";
  private static final String PROXY_KIND_CONSTRUCTOR = "constructor";
  private static final String PROXY_KIND_VIRTUAL = "virtual";

  static {
    try {
      indyBootstrapMethod =
          IndyBootstrapDispatcher.class.getMethod(
              "bootstrap",
              MethodHandles.Lookup.class,
              String.class,
              MethodType.class,
              Object[].class);

      MethodType bootstrapMethodType =
          MethodType.methodType(
              ConstantCallSite.class,
              MethodHandles.Lookup.class,
              String.class,
              MethodType.class,
              Object[].class);

      IndyBootstrapDispatcher.init(
          MethodHandles.lookup().findStatic(IndyBootstrap.class, "bootstrap", bootstrapMethodType));

      // Ensure that CallDepth is already loaded in case of bootstrapAdvice recursions with
      // ClassLoader.loadClass
      // This is required because CallDepth is a bootstrap class and therefore triggers our
      // ClassLoader.loadClass instrumentations
      Class.forName(CallDepth.class.getName());
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private IndyBootstrap() {}

  public static Method getIndyBootstrapMethod() {
    return indyBootstrapMethod;
  }

  @Nullable
  @SuppressWarnings({"unused", "removal"}) // SecurityManager and AccessController are deprecated
  private static ConstantCallSite bootstrap(
      MethodHandles.Lookup lookup,
      String adviceMethodName,
      MethodType adviceMethodType,
      Object[] args) {

    if (System.getSecurityManager() == null) {
      return internalBootstrap(lookup, adviceMethodName, adviceMethodType, args);
    }

    // callsite resolution needs privileged access to call Class#getClassLoader() and
    // MethodHandles$Lookup#findStatic
    return java.security.AccessController.doPrivileged(
        (PrivilegedAction<ConstantCallSite>)
            () -> internalBootstrap(lookup, adviceMethodName, adviceMethodType, args));
  }

  private static ConstantCallSite internalBootstrap(
      MethodHandles.Lookup lookup,
      String adviceMethodName,
      MethodType adviceMethodType,
      Object[] args) {
    try {
      String kind = (String) args[0];
      switch (kind) {
        case BOOTSTRAP_KIND_ADVICE:
          // See the getAdviceBootstrapArguments method for the argument definitions
          return bootstrapAdvice(
              lookup,
              adviceMethodName,
              adviceMethodType,
              (String) args[1],
              (String) args[2],
              (String) args[3]);
        case BOOTSTRAP_KIND_PROXY:
          // See getProxyFactory for the argument definitions
          return bootstrapProxyMethod(
              lookup,
              adviceMethodName,
              adviceMethodType,
              (String) args[1],
              (String) args[2],
              (String) args[3]);
        default:
          throw new IllegalArgumentException("Unknown bootstrapping kind: " + kind);
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, e.getMessage(), e);
      return null;
    }
  }

  private static ConstantCallSite bootstrapAdvice(
      MethodHandles.Lookup lookup,
      String adviceMethodName,
      MethodType invokedynamicMethodType,
      String moduleClassName,
      String adviceMethodDescriptor,
      String adviceClassName)
      throws NoSuchMethodException, IllegalAccessException, ClassNotFoundException {
    CallDepth callDepth = CallDepth.forClass(IndyBootstrap.class);
    try {
      if (callDepth.getAndIncrement() > 0) {
        // avoid re-entrancy and stack overflow errors, which may happen when bootstrapping an
        // instrumentation that also gets triggered during the bootstrap
        // for example, adding correlation ids to the thread context when executing logger.debug.
        logger.log(
            Level.FINE,
            "Nested instrumented invokedynamic instruction linkage detected for instrumented class {0} and advice {1}.{2}, the instrumentation might not work correctly",
            new Object[] {lookup.lookupClass().getName(), adviceClassName, adviceMethodName});
        logger.log(
            Level.FINE,
            "Stacktrace for nested invokedynamic instruction linkage:",
            new Throwable());
        return null;
      }

      InstrumentationModuleClassLoader instrumentationClassloader =
          IndyModuleRegistry.getInstrumentationClassLoader(
              moduleClassName, lookup.lookupClass().getClassLoader());

      // Advices are not inlined. They are loaded as normal classes by the
      // InstrumentationModuleClassloader and invoked via a method call from the instrumented method
      Class<?> adviceClass = instrumentationClassloader.loadClass(adviceClassName);
      MethodType actualAdviceMethodType =
          MethodType.fromMethodDescriptorString(adviceMethodDescriptor, instrumentationClassloader);

      MethodHandle methodHandle =
          instrumentationClassloader
              .getLookup()
              .findStatic(adviceClass, adviceMethodName, actualAdviceMethodType)
              .asType(invokedynamicMethodType);
      return new ConstantCallSite(methodHandle);
    } finally {
      callDepth.decrementAndGet();
    }
  }

  static Advice.BootstrapArgumentResolver.Factory getAdviceBootstrapArguments(
      InstrumentationModule instrumentationModule) {
    String moduleName = instrumentationModule.getClass().getName();
    return (adviceMethod, exit) ->
        (instrumentedType, instrumentedMethod) ->
            Arrays.asList(
                JavaConstant.Simple.ofLoaded(BOOTSTRAP_KIND_ADVICE),
                JavaConstant.Simple.ofLoaded(moduleName),
                JavaConstant.Simple.ofLoaded(getOriginalSignature(adviceMethod)),
                JavaConstant.Simple.ofLoaded(adviceMethod.getDeclaringType().getName()));
  }

  private static String getOriginalSignature(MethodDescription.InDefinedShape adviceMethod) {
    for (AnnotationDescription an : adviceMethod.getDeclaredAnnotations()) {
      if (OriginalDescriptor.class.getName().equals(an.getAnnotationType().getName())) {
        return (String) an.getValue("value").resolve();
      }
    }
    throw new IllegalStateException("OriginalSignature annotation is not present!");
  }

  private static ConstantCallSite bootstrapProxyMethod(
      MethodHandles.Lookup lookup,
      String proxyMethodName,
      MethodType expectedMethodType,
      String moduleClassName,
      String proxyClassName,
      String methodKind)
      throws NoSuchMethodException, IllegalAccessException, ClassNotFoundException {
    InstrumentationModuleClassLoader instrumentationClassloader =
        IndyModuleRegistry.getInstrumentationClassLoader(
            moduleClassName, lookup.lookupClass().getClassLoader());

    Class<?> proxiedClass = instrumentationClassloader.loadClass(proxyClassName);

    MethodHandle target;
    switch (methodKind) {
      case PROXY_KIND_STATIC:
        target =
            MethodHandles.publicLookup()
                .findStatic(proxiedClass, proxyMethodName, expectedMethodType);
        break;
      case PROXY_KIND_CONSTRUCTOR:
        target =
            MethodHandles.publicLookup()
                .findConstructor(proxiedClass, expectedMethodType.changeReturnType(void.class))
                .asType(expectedMethodType); // return type is the proxied class, but proxies expect
        // Object
        break;
      case PROXY_KIND_VIRTUAL:
        target =
            MethodHandles.publicLookup()
                .findVirtual(
                    proxiedClass, proxyMethodName, expectedMethodType.dropParameterTypes(0, 1))
                .asType(
                    expectedMethodType); // first argument type is the proxied class, but proxies
        // expect Object
        break;
      default:
        throw new IllegalStateException("unknown proxy method kind: " + methodKind);
    }
    return new ConstantCallSite(target);
  }

  /**
   * Creates a proxy factory for generating proxies for classes which are loaded by an {@link
   * InstrumentationModuleClassLoader} for the provided {@link InstrumentationModule}.
   *
   * @param instrumentationModule the instrumentation module used to load the proxied target classes
   * @return a factory for generating proxy classes
   */
  public static IndyProxyFactory getProxyFactory(InstrumentationModule instrumentationModule) {
    String moduleName = instrumentationModule.getClass().getName();
    return new IndyProxyFactory(
        getIndyBootstrapMethod(),
        (proxiedType, proxiedMethod) -> {
          String methodKind;
          if (proxiedMethod.isConstructor()) {
            methodKind = PROXY_KIND_CONSTRUCTOR;
          } else if (proxiedMethod.isMethod()) {
            if (proxiedMethod.isStatic()) {
              methodKind = PROXY_KIND_STATIC;
            } else {
              methodKind = PROXY_KIND_VIRTUAL;
            }
          } else {
            throw new IllegalArgumentException(
                "Unknown type of method: " + proxiedMethod.getName());
          }

          return Arrays.asList(
              JavaConstant.Simple.ofLoaded(BOOTSTRAP_KIND_PROXY),
              JavaConstant.Simple.ofLoaded(moduleName),
              JavaConstant.Simple.ofLoaded(proxiedType.getName()),
              JavaConstant.Simple.ofLoaded(methodKind));
        });
  }
}
