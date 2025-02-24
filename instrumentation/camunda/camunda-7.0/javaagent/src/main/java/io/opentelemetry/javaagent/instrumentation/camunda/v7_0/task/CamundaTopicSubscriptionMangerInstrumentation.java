/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.camunda.v7_0.task;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static io.opentelemetry.javaagent.instrumentation.camunda.v7_0.task.CamundaTaskSingletons.getInstumenter;
import static io.opentelemetry.javaagent.instrumentation.camunda.v7_0.task.CamundaTaskSingletons.getOpentelemetry;
import static net.bytebuddy.matcher.ElementMatchers.named;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.camunda.v7_0.common.CamundaCommonRequest;
import io.opentelemetry.instrumentation.camunda.v7_0.task.CamundaExternalTaskGetter;
import io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.util.Map;
import java.util.Optional;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.impl.ExternalTaskImpl;
import org.camunda.bpm.client.variable.impl.TypedValueField;
import org.camunda.bpm.client.variable.impl.TypedValues;
import org.camunda.bpm.client.variable.impl.VariableValue;

public class CamundaTopicSubscriptionMangerInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<ClassLoader> classLoaderOptimization() {
    return hasClassesNamed("org.camunda.bpm.client.topic.impl.TopicSubscriptionManager");
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("org.camunda.bpm.client.topic.impl.TopicSubscriptionManager");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        ElementMatchers.isMethod().and(named("handleExternalTask")),
        this.getClass().getName() + "$CamundaTopicSubscriptionMangerAdvice");
  }

  public static class CamundaTopicSubscriptionMangerAdvice {

    @SuppressWarnings({"rawtypes", "unused"})
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void addTracingEnter(
        @Advice.FieldValue("typedValues") TypedValues typedValues,
        @Advice.Argument(0) ExternalTask externalTask,
        @Advice.Local("request") CamundaCommonRequest request,
        @Advice.Local("otelParentScope") Scope parentScope,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope) {

      if (externalTask == null) {
        return;
      }

      request = new CamundaCommonRequest();
      request.setProcessDefinitionKey(Optional.ofNullable(externalTask.getProcessDefinitionKey()));
      request.setProcessInstanceId(Optional.ofNullable(externalTask.getProcessInstanceId()));
      request.setTopicName(Optional.ofNullable(externalTask.getTopicName()));
      request.setTopicWorkerId(Optional.ofNullable(externalTask.getWorkerId()));

      String id = externalTask.getTopicName() + " " + externalTask.getWorkerId();

      if (Java8BytecodeBridge.currentContext() == Java8BytecodeBridge.rootContext()) {
        // log
      }

      ExternalTaskImpl task = (ExternalTaskImpl) externalTask;

      Map<String, TypedValueField> variables = task.getVariables();

      Map<String, VariableValue> wrappedVariables =
          typedValues.wrapVariables(externalTask, variables);

      task.setReceivedVariableMap(wrappedVariables);

      Context parentContext =
          getOpentelemetry()
              .getPropagators()
              .getTextMapPropagator()
              .extract(
                  Java8BytecodeBridge.currentContext(),
                  externalTask,
                  new CamundaExternalTaskGetter());

      parentScope = parentContext.makeCurrent();

      if (getInstumenter().shouldStart(Java8BytecodeBridge.currentContext(), request)) {
        context = getInstumenter().start(Java8BytecodeBridge.currentContext(), request);
        scope = context.makeCurrent();
      }
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void closeTrace(
        @Advice.Local("otelParentScope") Scope parentScope,
        @Advice.Local("request") CamundaCommonRequest request,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope,
        @Advice.Thrown Throwable throwable) {

      if (context != null && scope != null) {
        getInstumenter().end(context, request, "NA", throwable);
        scope.close();
      }

      if (parentScope != null) {
        parentScope.close();
      }
    }
  }
}
