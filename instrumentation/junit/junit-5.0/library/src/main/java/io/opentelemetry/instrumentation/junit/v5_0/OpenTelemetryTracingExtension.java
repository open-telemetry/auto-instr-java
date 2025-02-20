/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.junit.v5_0;

import static io.opentelemetry.instrumentation.junit.v5_0.JunitConstants.JUNIT_OPENTELEMETRY_ROOT_CONTEXT;
import static io.opentelemetry.instrumentation.junit.v5_0.JunitConstants.JUNIT_OPENTELEMETRY_ROOT_SCOPE;
import static io.opentelemetry.instrumentation.junit.v5_0.JunitConstants.JUNIT_OPENTELEMETRY_TEST_CONTEXT;
import static io.opentelemetry.instrumentation.junit.v5_0.JunitConstants.JUNIT_OPENTELEMETRY_TEST_SCOPE;
import static io.opentelemetry.instrumentation.junit.v5_0.JunitConstants.JUNIT_SPAN_NAME;
import static io.opentelemetry.instrumentation.junit.v5_0.JunitConstants.JUNIT_TEST_ARGUMENTS;
import static io.opentelemetry.instrumentation.junit.v5_0.JunitConstants.JUNIT_TEST_LIFECYCLE;
import static io.opentelemetry.instrumentation.junit.v5_0.JunitConstants.JUNIT_TEST_RESULT;
import static io.opentelemetry.instrumentation.junit.v5_0.JunitConstants.OPENTELEMETRY_TRACING_EXTENSION_NAMESPACE;
import static io.opentelemetry.instrumentation.junit.v5_0.JunitSingletons.instrumenter;
import static io.opentelemetry.instrumentation.junit.v5_0.JunitTestResult.ABORTED;
import static io.opentelemetry.instrumentation.junit.v5_0.JunitTestResult.FAILED;
import static io.opentelemetry.instrumentation.junit.v5_0.JunitTestResult.SUCCESSFUL;
import static io.opentelemetry.instrumentation.junit.v5_0.TestLifecycle.AFTER_ALL;
import static org.junit.platform.commons.support.AnnotationSupport.isAnnotated;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import java.lang.reflect.Method;
import java.util.stream.Collectors;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.InvocationInterceptor;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;
import org.opentest4j.TestAbortedException;

final class OpenTelemetryTracingExtension
    implements InvocationInterceptor,
        BeforeAllCallback,
        BeforeEachCallback,
        AfterAllCallback,
        AfterEachCallback {

  @Override
  public void interceptBeforeAllMethod(
      Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    createInterceptorSpan(
        invocation, invocationContext, extensionContext, TestLifecycle.BEFORE_ALL);
  }

  @Override
  public void interceptAfterAllMethod(
      Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    createInterceptorSpan(invocation, invocationContext, extensionContext, AFTER_ALL);
  }

  @Override
  public void interceptBeforeEachMethod(
      Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    createInterceptorSpan(
        invocation, invocationContext, extensionContext, TestLifecycle.BEFORE_EACH);
  }

  @Override
  public <T> T interceptTestFactoryMethod(
      Invocation<T> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    return createInterceptorSpan(
        invocation, invocationContext, extensionContext, TestLifecycle.FACTORY_METHOD);
  }

  @Override
  public void interceptTestMethod(
      Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    createInterceptorSpan(invocation, invocationContext, extensionContext, TestLifecycle.TEST);
  }

  @Override
  public void interceptAfterEachMethod(
      Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    createInterceptorSpan(
        invocation, invocationContext, extensionContext, TestLifecycle.AFTER_EACH);
  }

  private static void setSystemProperties(ExtensionContext context) {
    if (isAnnotated(context.getRequiredTestClass(), JunitOpenTelemetryTracing.class)) {
      JunitOpenTelemetryTracing annotation =
          context.getRequiredTestClass().getAnnotation(JunitOpenTelemetryTracing.class);
      System.setProperty("otel.java.global-autoconfigure.enabled", annotation.enabled());
      System.setProperty("otel.traces.exporter", annotation.traceExporter());
      System.setProperty("otel.metrics.exporter", annotation.metricsExporter());
      System.setProperty("otel.logs.exporter", annotation.logExporter());
      System.setProperty("otel.exporter.otlp.endpoint", annotation.otlpEndpoint());
      System.setProperty("otel.service.name", JunitOpenTelemetryTracing.class.getSimpleName());
    }
  }

  private static <T> T createInterceptorSpan(
      Invocation<T> invocation,
      ReflectiveInvocationContext<?> invocationContext,
      ExtensionContext extensionContext,
      TestLifecycle lifecycle)
      throws Throwable {
    Context parentContext = Context.current();
    if (!instrumenter().shouldStart(parentContext, extensionContext)) {
      return invocation.proceed();
    }
    if (invocationContext != null && !invocationContext.getArguments().isEmpty()) {
      extensionContext
          .getStore(OPENTELEMETRY_TRACING_EXTENSION_NAMESPACE)
          .put(
              JUNIT_TEST_ARGUMENTS,
              invocationContext.getArguments().stream()
                  .map(String::valueOf)
                  .collect(Collectors.toList()));
    }
    extensionContext
        .getStore(OPENTELEMETRY_TRACING_EXTENSION_NAMESPACE)
        .put(JUNIT_TEST_LIFECYCLE, lifecycle.name());
    extensionContext
        .getStore(OPENTELEMETRY_TRACING_EXTENSION_NAMESPACE)
        .put(JUNIT_SPAN_NAME, lifecycle.name());
    Context context = instrumenter().start(parentContext, extensionContext);
    try (Scope scope = context.makeCurrent()) {
      T returnValue = invocation.proceed();
      if (lifecycle == TestLifecycle.TEST) {
        extensionContext
            .getStore(OPENTELEMETRY_TRACING_EXTENSION_NAMESPACE)
            .put(JUNIT_TEST_RESULT, SUCCESSFUL.name());
      }
      instrumenter().end(context, extensionContext, null, null);
      return returnValue;
    } catch (Throwable e) {
      if (e instanceof TestAbortedException) {
        extensionContext
            .getStore(OPENTELEMETRY_TRACING_EXTENSION_NAMESPACE)
            .put(JUNIT_TEST_RESULT, ABORTED.name());
      } else {
        extensionContext
            .getStore(OPENTELEMETRY_TRACING_EXTENSION_NAMESPACE)
            .put(JUNIT_TEST_RESULT, FAILED.name());
      }
      instrumenter().end(context, extensionContext, null, e);
      throw e;
    } finally {
      extensionContext
          .getStore(OPENTELEMETRY_TRACING_EXTENSION_NAMESPACE)
          .put(JUNIT_TEST_LIFECYCLE, null);
      extensionContext
          .getStore(OPENTELEMETRY_TRACING_EXTENSION_NAMESPACE)
          .put(JUNIT_SPAN_NAME, null);
      extensionContext
          .getStore(OPENTELEMETRY_TRACING_EXTENSION_NAMESPACE)
          .put(JUNIT_TEST_RESULT, null);
      extensionContext
          .getStore(OPENTELEMETRY_TRACING_EXTENSION_NAMESPACE)
          .put(JUNIT_TEST_ARGUMENTS, null);
    }
  }

  @Override
  public void afterAll(ExtensionContext extensionContext) {
    Context rootContext =
        extensionContext
            .getStore(OPENTELEMETRY_TRACING_EXTENSION_NAMESPACE)
            .get(JUNIT_OPENTELEMETRY_ROOT_CONTEXT, Context.class);
    Scope rootScope =
        extensionContext
            .getStore(OPENTELEMETRY_TRACING_EXTENSION_NAMESPACE)
            .get(JUNIT_OPENTELEMETRY_ROOT_SCOPE, Scope.class);
    instrumenter().end(rootContext, extensionContext, null, null);
    rootScope.close();
  }

  @Override
  public void afterEach(ExtensionContext extensionContext) {
    Context testContext =
        extensionContext
            .getStore(OPENTELEMETRY_TRACING_EXTENSION_NAMESPACE)
            .get(JUNIT_OPENTELEMETRY_TEST_CONTEXT, Context.class);
    Scope testScope =
        extensionContext
            .getStore(OPENTELEMETRY_TRACING_EXTENSION_NAMESPACE)
            .get(JUNIT_OPENTELEMETRY_TEST_SCOPE, Scope.class);
    instrumenter().end(testContext, extensionContext, null, null);
    testScope.close();
  }

  @Override
  public void beforeAll(ExtensionContext extensionContext) {
    setSystemProperties(
        extensionContext); // TODO: требуется ли сохранять переменные до установки и сбрасывать их к
    // первоначальному состоянию после окончания теста??
    Context parentContext = Context.current();
    if (!instrumenter().shouldStart(parentContext, extensionContext)) {
      return;
    }
    Context rootContext = instrumenter().start(parentContext, extensionContext);
    Scope rootScope = rootContext.makeCurrent();
    extensionContext
        .getStore(OPENTELEMETRY_TRACING_EXTENSION_NAMESPACE)
        .put(JUNIT_OPENTELEMETRY_ROOT_CONTEXT, rootContext);
    extensionContext
        .getStore(OPENTELEMETRY_TRACING_EXTENSION_NAMESPACE)
        .put(JUNIT_OPENTELEMETRY_ROOT_SCOPE, rootScope);
  }

  @Override
  public void beforeEach(ExtensionContext extensionContext) {
    Context parentContext = Context.current();
    if (!instrumenter().shouldStart(parentContext, extensionContext)) {
      return;
    }
    extensionContext
        .getStore(OPENTELEMETRY_TRACING_EXTENSION_NAMESPACE)
        .put(JUNIT_TEST_LIFECYCLE, null);
    Context testContext = instrumenter().start(parentContext, extensionContext);
    Scope testScope = testContext.makeCurrent();
    extensionContext
        .getStore(OPENTELEMETRY_TRACING_EXTENSION_NAMESPACE)
        .put(JUNIT_OPENTELEMETRY_TEST_CONTEXT, testContext);
    extensionContext
        .getStore(OPENTELEMETRY_TRACING_EXTENSION_NAMESPACE)
        .put(JUNIT_OPENTELEMETRY_TEST_SCOPE, testScope);
  }

  @Override
  public void interceptTestTemplateMethod(
      Invocation<Void> invocation,
      ReflectiveInvocationContext<Method> invocationContext,
      ExtensionContext extensionContext)
      throws Throwable {
    createInterceptorSpan(invocation, invocationContext, extensionContext, TestLifecycle.TEST);
  }
}
