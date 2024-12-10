/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.ktor.v2_0;

import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.named;

import io.ktor.server.application.Application;
import io.ktor.server.application.ApplicationPluginKt;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.ktor.v2_0.common.internal.KtorBuilderUtil;
import io.opentelemetry.instrumentation.ktor.v2_0.common.server.AbstractKtorServerTelemetryBuilder;
import io.opentelemetry.instrumentation.ktor.v2_0.server.KtorServerTelemetryBuilderKt;
import io.opentelemetry.javaagent.bootstrap.internal.AgentCommonConfig;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class ServerInstrumentation implements TypeInstrumentation {
  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("io.ktor.server.engine.ApplicationEngineEnvironmentReloading");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isConstructor(), this.getClass().getName() + "$ConstructorAdvice");
  }

  @SuppressWarnings("unused")
  public static class ConstructorAdvice {

    @Advice.OnMethodExit
    public static void onExit(@Advice.FieldValue("_applicationInstance") Application application) {
      ApplicationPluginKt.install(
          application, KtorServerTelemetryBuilderKt.getKtorServerTelemetry(), new SetupFunction());
    }
  }

  public static class SetupFunction
      implements Function1<AbstractKtorServerTelemetryBuilder, kotlin.Unit> {

    @Override
    public Unit invoke(AbstractKtorServerTelemetryBuilder builder) {
      builder.setOpenTelemetry(GlobalOpenTelemetry.get());
      KtorBuilderUtil.serverBuilderExtractor.invoke(builder).configure(AgentCommonConfig.get());
      return kotlin.Unit.INSTANCE;
    }
  }
}
