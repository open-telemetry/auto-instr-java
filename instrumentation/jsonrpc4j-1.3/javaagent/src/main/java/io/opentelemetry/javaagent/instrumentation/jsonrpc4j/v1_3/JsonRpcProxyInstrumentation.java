/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.jsonrpc4j.v1_3;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static io.opentelemetry.javaagent.instrumentation.jsonrpc4j.v1_3.JsonRpcSingletons.instrumentCreateClientProxy;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isPrivate;
import static net.bytebuddy.matcher.ElementMatchers.isStatic;
import static net.bytebuddy.matcher.ElementMatchers.named;

import com.googlecode.jsonrpc4j.IJsonRpcClient;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class JsonRpcProxyInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<ClassLoader> classLoaderOptimization() {
    return hasClassesNamed("com.googlecode.jsonrpc4j.ProxyUtil");
  }

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("com.googlecode.jsonrpc4j.ProxyUtil");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod().and(isStatic()).and(isPrivate()).and(named("createClientProxy")),
        this.getClass().getName() + "$CreateClientProxyAdvice");
  }

  @SuppressWarnings({"unused"})
  public static class CreateClientProxyAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static <T> void onExit(
        @Advice.Argument(0) ClassLoader classLoader,
        @Advice.Argument(1) Class<T> proxyInterface,
        @Advice.Argument(2) IJsonRpcClient client,
        @Advice.Argument(3) Map<String, String> extraHeaders,
        @Advice.Return(readOnly = false) Object proxy) {

      proxy = instrumentCreateClientProxy(classLoader, proxyInterface, client, extraHeaders, proxy);
    }
  }
}
