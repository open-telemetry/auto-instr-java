/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.camunda.v7_0.behavior;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.bytebuddy.matcher.ElementMatcher;

@AutoService(InstrumentationModule.class)
public class CamundaCommonBehaviorModule extends InstrumentationModule {

  public CamundaCommonBehaviorModule() {
    super("camunda", "camunda-behavior", "camunda-behavior-7_18");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Collections.singletonList(new CamundaCommonBehaviorInstrumentation());
  }

  @Override
  public ElementMatcher.Junction<ClassLoader> classLoaderMatcher() {
    return hasClassesNamed(
        "org.camunda.bpm.engine.impl.bpmn.behavior.TaskActivityBehavior",
        "org.camunda.bpm.engine.impl.bpmn.behavior.NoneEndEventActivityBehavior",
        "org.camunda.bpm.engine.impl.bpmn.behavior.ErrorEndEventActivityBehavior");
  }

  String[] helperClassnames = {
    "io.opentelemetry.javaagent.instrumentation.camunda.v7_0.behavior",
    "io.opentelemetry.instrumentation.camunda.v7_0.behavior",
    "io.opentelemetry.instrumentation.camunda.v7_0.common"
  };

  @Override
  public boolean isHelperClass(String classname) {
    return super.isHelperClass(classname)
        || Arrays.stream(helperClassnames).anyMatch(c -> classname.startsWith(c));
  }
}
