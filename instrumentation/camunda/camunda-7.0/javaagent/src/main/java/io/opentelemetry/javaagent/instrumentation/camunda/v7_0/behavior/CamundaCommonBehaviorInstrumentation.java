/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.camunda.v7_0.behavior;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasSuperType;
import static io.opentelemetry.javaagent.instrumentation.camunda.v7_0.behavior.CamundaBehaviorSingletons.getInstumenter;
import static io.opentelemetry.javaagent.instrumentation.camunda.v7_0.behavior.CamundaBehaviorSingletons.getOpentelemetry;
import static net.bytebuddy.matcher.ElementMatchers.named;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.camunda.v7_0.behavior.CamundaActivityExecutionGetter;
import io.opentelemetry.instrumentation.camunda.v7_0.behavior.CamundaActivityExecutionLocalSetter;
import io.opentelemetry.instrumentation.camunda.v7_0.common.CamundaCommonRequest;
import io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.util.Optional;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.camunda.bpm.model.bpmn.instance.CompensateEventDefinition;
import org.camunda.bpm.model.bpmn.instance.EndEvent;
import org.camunda.bpm.model.bpmn.instance.ErrorEventDefinition;
import org.camunda.bpm.model.bpmn.instance.EventDefinition;
import org.camunda.bpm.model.bpmn.instance.Gateway;
import org.camunda.bpm.model.bpmn.instance.TerminateEventDefinition;

public class CamundaCommonBehaviorInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<ClassLoader> classLoaderOptimization() {
    return hasClassesNamed("org.camunda.bpm.engine.impl.bpmn.behavior.TaskActivityBehavior");
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return hasSuperType(named("org.camunda.bpm.engine.impl.bpmn.behavior.TaskActivityBehavior"))
        .or(named("org.camunda.bpm.engine.impl.bpmn.behavior.ExternalTaskActivityBehavior"))
        .or(named("org.camunda.bpm.engine.impl.bpmn.behavior.TerminateEndEventActivityBehavior"))
        .or(named("org.camunda.bpm.engine.impl.bpmn.behavior.NoneEndEventActivityBehavior"))
        .or(named("org.camunda.bpm.engine.impl.bpmn.behavior.ErrorEndEventActivityBehavior"));
    // elements that have been tested with instrumentation, eventually this can be
    // replaced by the supertype AbstractBpmnActivityBehavior, once all elements
    // instrumentations have been certified and instrumented, but will need to make
    // sure its not callable element as the instrumentation should remain separate
    // due to logic
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        ElementMatchers.isMethod().and(ElementMatchers.named("execute")),
        this.getClass().getName() + "$CamundaCommonBehaviorAdvice");
  }

  @SuppressWarnings("unused")
  public static class CamundaCommonBehaviorAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void addTracingEnter(
        @Advice.Argument(0) ActivityExecution execution,
        @Advice.This Object target,
        @Advice.Local("request") CamundaCommonRequest request,
        @Advice.Local("otelParentScope") Scope parentScope,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope) {

      if (execution == null) {
        return;
      }

      request = new CamundaCommonRequest();
      request.setProcessDefinitionId(Optional.ofNullable(execution.getProcessDefinitionId()));
      request.setProcessInstanceId(Optional.ofNullable(execution.getProcessInstanceId()));
      request.setActivityId(Optional.ofNullable(execution.getCurrentActivityId()));
      request.setBusinessKey(Optional.ofNullable(execution.getProcessBusinessKey()));

      if (execution.getBpmnModelElementInstance() != null) {
        // TODO lambda does not work due to access modifier
        if (execution.getBpmnModelElementInstance() instanceof EndEvent) {
          EndEvent e = (EndEvent) execution.getBpmnModelElementInstance();

          if (e.getEventDefinitions() == null || e.getEventDefinitions().isEmpty()) {
            request.setActivityName(Optional.of("End"));
          }
          for (EventDefinition ed : e.getEventDefinitions()) {
            if (ed instanceof TerminateEventDefinition) {
              request.setActivityName(Optional.of("End"));
            } else if (ed instanceof ErrorEventDefinition) {
              request.setActivityName(Optional.of("Error End"));
            } else if (ed instanceof CompensateEventDefinition) {
              request.setActivityName(Optional.of("Compensation End"));
            } else {
              request.setActivityName(Optional.of("End"));
            }
          }
        } else if (execution.getBpmnModelElementInstance() instanceof Gateway) {
          // TODO
        } else {
          request.setActivityName(Optional.ofNullable(execution.getCurrentActivityName()));
        }
      } else {
        request.setActivityName(Optional.ofNullable(execution.getCurrentActivityName()));
      }

      Context parentContext =
          getOpentelemetry()
              .getPropagators()
              .getTextMapPropagator()
              .extract(
                  Java8BytecodeBridge.currentContext(),
                  execution,
                  new CamundaActivityExecutionGetter());

      parentScope = parentContext.makeCurrent();

      if (getInstumenter().shouldStart(Java8BytecodeBridge.currentContext(), request)) {
        context = getInstumenter().start(Java8BytecodeBridge.currentContext(), request);
        scope = context.makeCurrent();

        if (target.getClass()
            == org.camunda.bpm.engine.impl.bpmn.behavior.ExternalTaskActivityBehavior.class) {

          getOpentelemetry()
              .getPropagators()
              .getTextMapPropagator()
              .inject(context, execution, new CamundaActivityExecutionLocalSetter());
        }
      }
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void closeTrace(
        @Advice.Local("request") CamundaCommonRequest request,
        @Advice.This Object target,
        @Advice.Local("otelParentScope") Scope parentScope,
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
