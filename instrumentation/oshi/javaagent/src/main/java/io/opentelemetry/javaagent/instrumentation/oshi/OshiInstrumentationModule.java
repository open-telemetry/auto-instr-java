/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.oshi;

import static io.opentelemetry.javaagent.extension.matcher.ClassLoaderMatcher.hasClassesNamed;
import static java.util.Collections.singletonList;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.isStatic;
import static net.bytebuddy.matcher.ElementMatchers.named;

import com.google.auto.service.AutoService;
import io.opentelemetry.instrumentation.oshi.SystemMetrics;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.util.List;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

@AutoService(InstrumentationModule.class)
public class OshiInstrumentationModule extends InstrumentationModule {

  public OshiInstrumentationModule() {
    super("oshi");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return singletonList(new SystemInfoInstrumentation());
  }

  public static class SystemInfoInstrumentation implements TypeInstrumentation {
    @Override
    public ElementMatcher<ClassLoader> classLoaderOptimization() {
      return hasClassesNamed("oshi.SystemInfo");
    }

    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
      return named("oshi.SystemInfo");
    }

    @Override
    public void transform(TypeTransformer transformer) {
      transformer.applyAdviceToMethod(
          isMethod().and(isPublic()).and(isStatic()).and(named("getCurrentPlatformEnum")),
          OshiInstrumentationModule.class.getName() + "$OshiInstrumentationAdvice");
    }
  }

  public static class OshiInstrumentationAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void onEnter() {
      SystemMetrics.registerObservers();
    }
  }
}
