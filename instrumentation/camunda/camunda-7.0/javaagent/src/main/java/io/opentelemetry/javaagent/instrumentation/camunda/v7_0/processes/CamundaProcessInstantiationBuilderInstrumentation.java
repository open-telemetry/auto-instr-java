/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.camunda.v7_0.processes;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.camunda.v7_0.common.CamundaCommonRequest;
import io.opentelemetry.instrumentation.camunda.v7_0.processes.CamundaActivityInstantiationBuilderSetter;
import io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.util.Optional;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.camunda.bpm.engine.impl.ProcessInstanceModificationBuilderImpl;
import org.camunda.bpm.engine.runtime.ProcessInstance;

public class CamundaProcessInstantiationBuilderInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<ClassLoader> classLoaderOptimization() {
    return hasClassesNamed("org.camunda.bpm.engine.runtime.ProcessInstantiationBuilder");
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return hasSuperType(named("org.camunda.bpm.engine.runtime.ProcessInstantiationBuilder"));
  }

  @Override
  public void transform(TypeTransformer transformer) {
    // comment out for now because it causes duplicate spans
    // transformer.applyAdviceToMethod(isMethod().and(named("execute")),
    // this.getClass().getName() +
    // "$CamundaProcessInstantiationBuilderExecuteAdvice");

    transformer.applyAdviceToMethod(
        isMethod().and(named("executeWithVariablesInReturn")),
        this.getClass().getName()
            + "$CamundaProcessInstantiationBuilderExecuteWithVariablesAdvice");
  }

  @SuppressWarnings("unused")
  public static class CamundaProcessInstantiationBuilderExecuteWithVariablesAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void addTracingEnter(
        @Advice.FieldValue("modificationBuilder")
            ProcessInstanceModificationBuilderImpl modificationBuilder,
        @Advice.FieldValue("processDefinitionKey") String processDefinitionKey,
        @Advice.FieldValue("businessKey") String businessKey,
        @Advice.Local("request") CamundaCommonRequest request,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope) {

      request = new CamundaCommonRequest();
      request.setProcessDefinitionKey(Optional.ofNullable(processDefinitionKey));

      Context parentContext = Java8BytecodeBridge.currentContext();

      if (CamundaProcessSingletons.getInstumenter().shouldStart(parentContext, request)) {
        context = CamundaProcessSingletons.getInstumenter().start(parentContext, request);
        scope = context.makeCurrent();

        SpanContext currentSpanContext =
            Java8BytecodeBridge.spanFromContext(context).getSpanContext();
        if (currentSpanContext.isValid()) {
          CamundaProcessSingletons.getOpentelemetry()
              .getPropagators()
              .getTextMapPropagator()
              .inject(
                  context, modificationBuilder, new CamundaActivityInstantiationBuilderSetter());
        }
      }
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void closeTrace(
        @Advice.FieldValue("processDefinitionKey") String processDefinitionKey,
        @Advice.Local("request") CamundaCommonRequest request,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope,
        @Advice.Thrown Throwable throwable,
        @Advice.Return ProcessInstance pi) {

      if (context != null && scope != null) {
        CamundaProcessSingletons.getInstumenter().end(context, request, "NA", throwable);
        scope.close();
      }
    }
  }
}
