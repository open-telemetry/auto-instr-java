/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.jdbc;

import static io.opentelemetry.javaagent.instrumentation.jdbc.JdbcTracer.tracer;
import static io.opentelemetry.javaagent.tooling.ClassLoaderMatcher.hasClassesNamed;
import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.AgentElementMatchers.implementsInterface;
import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import io.opentelemetry.javaagent.instrumentation.api.CallDepthThreadLocalMap.Depth;
import io.opentelemetry.javaagent.tooling.TypeInstrumentation;
import java.sql.Statement;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

final class StatementInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<ClassLoader> classLoaderOptimization() {
    return hasClassesNamed("java.sql.Statement");
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return implementsInterface(named("java.sql.Statement"));
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    return singletonMap(
        nameStartsWith("execute").and(takesArgument(0, String.class)).and(isPublic()),
        StatementInstrumentation.class.getName() + "$StatementAdvice");
  }

  public static class StatementAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void onEnter(
        @Advice.Argument(0) String sql,
        @Advice.This Statement statement,
        @Advice.Local("otelSpan") Span span,
        @Advice.Local("otelScope") Scope scope,
        @Advice.Local("otelCallDepth") Depth callDepth) {

      callDepth = tracer().getCallDepth();
      if (callDepth.getAndIncrement() == 0) {
        span = tracer().startSpan(statement, sql);
        if (span != null) {
          scope = tracer().startScope(span);
        }
      }
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void stopSpan(
        @Advice.Thrown Throwable throwable,
        @Advice.Local("otelSpan") Span span,
        @Advice.Local("otelScope") Scope scope,
        @Advice.Local("otelCallDepth") Depth callDepth) {
      if (callDepth.decrementAndGet() == 0 && scope != null) {
        scope.close();
        if (throwable == null) {
          tracer().end(span);
        } else {
          tracer().endExceptionally(span, throwable);
        }
      }
    }
  }
}
