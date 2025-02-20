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

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.camunda.v7_0.behavior.CamundaActivityExecutionGetter;
import io.opentelemetry.instrumentation.camunda.v7_0.behavior.CamundaVariableMapSetter;
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
import org.camunda.bpm.engine.variable.VariableMap;

public class CamundaCallableElementActivityBehaviorInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<ClassLoader> classLoaderOptimization() {
    return hasClassesNamed(
        "org.camunda.bpm.engine.impl.bpmn.behavior.CallableElementActivityBehavior");
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return hasSuperType(
        named("org.camunda.bpm.engine.impl.bpmn.behavior.CallableElementActivityBehavior"));
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        ElementMatchers.isMethod().and(ElementMatchers.named("startInstance")),
        this.getClass().getName() + "$CamundaCallableElementActivityBehaviorAdvice");
  }

  @SuppressWarnings("unused")
  public static class CamundaCallableElementActivityBehaviorAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void addTracingEnter(
        @Advice.Argument(0) ActivityExecution execution,
        @Advice.Argument(1) VariableMap variables,
        @Advice.Local("request") CamundaCommonRequest request,
        @Advice.Local("otelParentScope") Scope parentScope,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope) {

      if (execution == null) {
        // log warning
        return;
      }

      request = new CamundaCommonRequest();
      request.setProcessDefinitionId(Optional.ofNullable(execution.getProcessDefinitionId()));
      request.setProcessInstanceId(Optional.ofNullable(execution.getProcessInstanceId()));
      request.setActivityId(Optional.ofNullable(execution.getCurrentActivityId()));
      request.setActivityName(Optional.ofNullable(execution.getCurrentActivityName()));
      request.setBusinessKey(Optional.ofNullable(execution.getProcessBusinessKey()));

      if (Java8BytecodeBridge.currentContext() == Java8BytecodeBridge.rootContext()) {
        // log
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

        // Inject subflow trace context as pi variables so they are propagated and
        // accessible

        SpanContext currentSpanContext =
            Java8BytecodeBridge.spanFromContext(context).getSpanContext();
        if (currentSpanContext.isValid()) {
          getOpentelemetry()
              .getPropagators()
              .getTextMapPropagator()
              .inject(context, variables, new CamundaVariableMapSetter());
        }
      }
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void closeTrace(
        @Advice.Argument(0) ActivityExecution execution,
        @Advice.Local("request") CamundaCommonRequest request,
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
