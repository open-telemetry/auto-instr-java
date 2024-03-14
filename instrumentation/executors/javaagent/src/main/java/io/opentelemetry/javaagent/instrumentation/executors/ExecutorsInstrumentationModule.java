/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.executors;

import static java.util.Arrays.asList;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import java.util.List;

@AutoService(InstrumentationModule.class)
public class ExecutorsInstrumentationModule extends InstrumentationModule {

  public ExecutorsInstrumentationModule() {
    super("executors");
  }

  @Override
  public boolean isIndyModule() {
    return false;
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return asList(
        new CallableInstrumentation(),
        new FutureInstrumentation(),
        new JavaExecutorInstrumentation(),
        new JavaForkJoinTaskInstrumentation(),
        new RunnableInstrumentation(),
        new ThreadPoolExecutorInstrumentation(),
        new ThreadPoolExtendingExecutorInstrumentation());
  }
}
