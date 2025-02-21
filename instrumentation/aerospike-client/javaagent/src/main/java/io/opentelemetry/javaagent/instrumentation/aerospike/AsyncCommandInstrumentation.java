/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.aerospike;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static net.bytebuddy.matcher.ElementMatchers.hasSuperClass;
import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import com.aerospike.client.AerospikeException;
import com.aerospike.client.cluster.Node;
import com.aerospike.client.command.Command;
import io.opentelemetry.instrumentation.api.util.VirtualField;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import io.opentelemetry.javaagent.instrumentation.aerospike.internal.AerospikeRequestContext;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class AsyncCommandInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<ClassLoader> classLoaderOptimization() {
    return hasClassesNamed("com.aerospike.client.async.AsyncCommand");
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return hasSuperClass(named("com.aerospike.client.async.AsyncCommand"));
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isConstructor().and(isPublic()), this.getClass().getName() + "$ConstructorAdvice");

    transformer.applyAdviceToMethod(
        isMethod().and(named("getNode")), this.getClass().getName() + "$GetNodeAdvice");

    transformer.applyAdviceToMethod(
        isMethod().and(named("onSuccess")), this.getClass().getName() + "$GetOnSuccessAdvice");

    transformer.applyAdviceToMethod(
        isMethod()
            .and(
                named("onFailure")
                    .and(takesArgument(0, named("com.aerospike.client.AerospikeException")))),
        this.getClass().getName() + "$GetOnFailureAdvice");
  }

  @SuppressWarnings("unused")
  public static class ConstructorAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void getCommand(@Advice.This Command command) {
      VirtualField<Command, AerospikeRequestContext> virtualField =
          VirtualField.find(Command.class, AerospikeRequestContext.class);
      AerospikeRequestContext requestContext = virtualField.get(command);
      // If the AerospikeRequestContext is already there in VirtualField then we do not need
      // to override it when constructor of other subclasses of AsyncCommand executes as
      // AsyncCommand follows multilevel inheritance.
      if (requestContext != null) {
        return;
      }
      requestContext = AerospikeRequestContext.current();
      if (requestContext == null) {
        return;
      }
      virtualField.set(command, requestContext);
      requestContext.detachContext();
    }
  }

  @SuppressWarnings("unused")
  public static class GetNodeAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void getNode(@Advice.Return Node node, @Advice.This Command command) {
      VirtualField<Command, AerospikeRequestContext> virtualField =
          VirtualField.find(Command.class, AerospikeRequestContext.class);
      AerospikeRequestContext requestContext = virtualField.get(command);
      if (requestContext == null || requestContext.getRequest().getNode() != null) {
        return;
      }
      requestContext.getRequest().setNode(node);
    }
  }

  @SuppressWarnings("unused")
  public static class GetOnSuccessAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void stopInstrumentation(@Advice.This Command command) {
      VirtualField<Command, AerospikeRequestContext> virtualField =
          VirtualField.find(Command.class, AerospikeRequestContext.class);
      AerospikeRequestContext requestContext = virtualField.get(command);
      if (requestContext == null) {
        return;
      }
      virtualField.set(command, null);
      requestContext.endSpan();
    }
  }

  @SuppressWarnings("unused")
  public static class GetOnFailureAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void stopInstrumentation(
        @Advice.Argument(0) AerospikeException ae, @Advice.This Command command) {
      VirtualField<Command, AerospikeRequestContext> virtualField =
          VirtualField.find(Command.class, AerospikeRequestContext.class);
      AerospikeRequestContext requestContext = virtualField.get(command);
      if (requestContext == null) {
        return;
      }
      virtualField.set(command, null);
      requestContext.setThrowable(ae);
      requestContext.endSpan();
    }
  }
}
