/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apolloconfig.apolloclient.v1_0;

import static io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge.currentContext;
import static io.opentelemetry.javaagent.instrumentation.apolloconfig.apolloclient.v1_0.ApolloConfigSingletons.REPOSITORY_CHANGE_REPEAT_CONTEXT_KEY;
import static io.opentelemetry.javaagent.instrumentation.apolloconfig.apolloclient.v1_0.ApolloConfigSingletons.instrumenter;
import static net.bytebuddy.matcher.ElementMatchers.named;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class ApolloRepositoryChangeInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("com.ctrip.framework.apollo.internals.AbstractConfigRepository");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        named("fireRepositoryChange"), this.getClass().getName() + "$ApolloRepositoryChangeAdvice");
  }

  @SuppressWarnings("unused")
  public static class ApolloRepositoryChangeAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void onEnter(
        @Advice.Argument(value = 0) String namespace,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope) {
      Context parentContext = currentContext();
      String repeat = parentContext.get(REPOSITORY_CHANGE_REPEAT_CONTEXT_KEY);
      if (repeat != null) {
        return;
      }
      if (!instrumenter().shouldStart(parentContext, namespace)) {
        return;
      }

      context = instrumenter().start(parentContext, namespace);
      context = context.with(REPOSITORY_CHANGE_REPEAT_CONTEXT_KEY, "1");
      scope = context.makeCurrent();
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void onExit(
        @Advice.Argument(value = 0) String namespace,
        @Advice.Thrown Throwable throwable,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope) {
      if (scope == null) {
        return;
      }
      scope.close();
      instrumenter().end(context, namespace, null, throwable);
    }
  }
}
