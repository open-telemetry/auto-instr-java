/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.nacos.client.v2_0_0.instrumentations;

import static io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge.currentContext;
import static io.opentelemetry.javaagent.instrumentation.nacos.client.v2_0_0.NacosClientSingletons.instrumenter;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;

import com.alibaba.nacos.api.remote.request.Request;
import com.alibaba.nacos.api.remote.response.Response;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import io.opentelemetry.javaagent.instrumentation.nacos.client.v2_0_0.NacosClientRequest;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class GrpcConnectionInstrumentation implements TypeInstrumentation {
  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("com.alibaba.nacos.common.remote.client.grpc.GrpcConnection");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod()
            .and(isPublic())
            .and(named("request"))
            .and(returns(named("com.alibaba.nacos.api.remote.response.Response"))),
        GrpcConnectionInstrumentation.class.getName() + "$GrpcConnectionRequestAdvice");
  }

  public static class GrpcConnectionRequestAdvice {
    @SuppressWarnings("unused")
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void requestEnter(
        @Advice.This Object thisObject,
        @Advice.Argument(0) Request request,
        @Advice.Local("otelRequest") NacosClientRequest nacosClientRequest,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope) {
      Context parentContext = currentContext();
      nacosClientRequest =
          NacosClientRequest.createRequest("request", thisObject.getClass(), request);
      if (!instrumenter().shouldStart(parentContext, nacosClientRequest)) {
        return;
      }
      context = instrumenter().start(parentContext, nacosClientRequest);
      scope = context.makeCurrent();
    }

    @SuppressWarnings("unused")
    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void requestExit(
        @Advice.Return Response response,
        @Advice.Thrown Throwable throwable,
        @Advice.Local("otelRequest") NacosClientRequest nacosClientRequest,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope) {
      if (scope == null) {
        return;
      }
      scope.close();
      instrumenter().end(context, nacosClientRequest, response, throwable);
    }
  }
}
