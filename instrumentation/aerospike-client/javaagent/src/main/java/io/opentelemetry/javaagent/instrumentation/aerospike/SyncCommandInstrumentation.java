/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.aerospike;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static net.bytebuddy.matcher.ElementMatchers.hasSuperClass;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;

import com.aerospike.client.cluster.Node;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import io.opentelemetry.javaagent.instrumentation.aerospike.internal.AerospikeRequestContext;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class SyncCommandInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<ClassLoader> classLoaderOptimization() {
    return hasClassesNamed("com.aerospike.client.command.SyncCommand");
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return hasSuperClass(named("com.aerospike.client.command.SyncCommand"));
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod().and(named("getNode")), this.getClass().getName() + "$GetNodeAdvice");
  }

  @SuppressWarnings("unused")
  public static class GetNodeAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void getNode(@Advice.Return Node node) {
      AerospikeRequestContext requestContext = AerospikeRequestContext.current();
      if (requestContext == null || requestContext.getRequest().getNode() != null) {
        return;
      }
      requestContext.getRequest().setNode(node);
    }
  }
}
