/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.ktor.v2_0;

import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.ktor.client.HttpClientConfig;
import io.ktor.client.engine.HttpClientEngineConfig;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.ktor.v2_0.client.KtorClientTelemetry;
import io.opentelemetry.instrumentation.ktor.v2_0.client.KtorClientTelemetryBuilder;
import io.opentelemetry.instrumentation.ktor.v2_0.common.internal.KtorBuilderUtil;
import io.opentelemetry.javaagent.bootstrap.internal.AgentCommonConfig;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class HttpClientInstrumentation implements TypeInstrumentation {
  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("io.ktor.client.HttpClient");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isConstructor()
            .and(takesArguments(2))
            .and(takesArgument(1, named("io.ktor.client.HttpClientConfig"))),
        this.getClass().getName() + "$ConstructorAdvice");
  }

  @SuppressWarnings("unused")
  public static class ConstructorAdvice {

    @Advice.OnMethodEnter
    public static void onEnter(
        @Advice.Argument(1) HttpClientConfig<HttpClientEngineConfig> httpClientConfig) {
      httpClientConfig.install(KtorClientTelemetry.Companion, new SetupFunction());
    }
  }

  public static class SetupFunction implements Function1<KtorClientTelemetryBuilder, Unit> {

    @Override
    public Unit invoke(KtorClientTelemetryBuilder builder) {
      builder.setOpenTelemetry(GlobalOpenTelemetry.get());
      KtorBuilderUtil.clientBuilderExtractor.invoke(builder).configure(AgentCommonConfig.get());
      return kotlin.Unit.INSTANCE;
    }
  }
}
