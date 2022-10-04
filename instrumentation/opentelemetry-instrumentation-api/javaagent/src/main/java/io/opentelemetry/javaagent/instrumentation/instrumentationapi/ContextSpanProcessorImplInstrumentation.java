/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.instrumentationapi;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import application.io.opentelemetry.api.trace.Span;
import application.io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.internal.ContextSpanProcessorImpl;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import io.opentelemetry.javaagent.instrumentation.opentelemetryapi.context.AgentContextStorage;
import java.util.function.BiConsumer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

final class ContextSpanProcessorImplInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named(
        "application.io.opentelemetry.instrumentation.api.internal.ContextSpanProcessorImpl");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        named("storeInContext")
            .and(takesArgument(0, named("application.io.opentelemetry.context.Context")))
            .and(takesArgument(1, BiConsumer.class)),
        this.getClass().getName() + "$StoreInContextAdvice");
    transformer.applyAdviceToMethod(
        named("fromContextOrNull")
            .and(takesArgument(0, named("application.io.opentelemetry.context.Context"))),
        this.getClass().getName() + "$FromContextOrNullAdvice");
  }

  @SuppressWarnings("unused")
  public static class StoreInContextAdvice {
    @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
    public static Object onEnter() {
      return null;
    }

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onExit(
        @Advice.Argument(0) Context applicationContext,
        @Advice.Argument(1) BiConsumer<Context, Span> processor,
        @Advice.Return(readOnly = false) Context newApplicationContext) {

      io.opentelemetry.context.Context agentContext =
          AgentContextStorage.getAgentContext(applicationContext);

      BiConsumer<io.opentelemetry.context.Context, io.opentelemetry.api.trace.Span> agentProcessor =
          ContextSpanProcessorWrapper.wrap(processor);
      io.opentelemetry.context.Context newAgentContext =
          ContextSpanProcessorImpl.storeInContext(agentContext, agentProcessor);

      newApplicationContext = AgentContextStorage.toApplicationContext(newAgentContext);
    }
  }

  @SuppressWarnings("unused")
  public static class FromContextOrNullAdvice {
    @Advice.OnMethodEnter(skipOn = Advice.OnDefaultValue.class)
    public static Object onEnter() {
      return null;
    }

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onExit(
        @Advice.Argument(0) Context applicationContext,
        @Advice.Return(readOnly = false) BiConsumer<Context, Span> processor) {

      io.opentelemetry.context.Context agentContext =
          AgentContextStorage.getAgentContext(applicationContext);
      BiConsumer<io.opentelemetry.context.Context, io.opentelemetry.api.trace.Span> agentProcessor =
          ContextSpanProcessorImpl.fromContextOrNull(agentContext);
      processor = ContextSpanProcessorWrapper.unwrap(agentProcessor);
    }
  }
}
