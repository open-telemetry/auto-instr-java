/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.restlet.v1_0;

import static io.opentelemetry.instrumentation.api.instrumenter.http.HttpRouteSource.CONTROLLER;
import static io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge.currentContext;
import static io.opentelemetry.javaagent.instrumentation.restlet.v1_0.RestletSingletons.serverSpanName;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import io.opentelemetry.instrumentation.api.instrumenter.http.HttpRouteHolder;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.restlet.Route;
import org.restlet.data.Request;

public class RouteInstrumentation implements TypeInstrumentation {
  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("org.restlet.Route");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod()
            .and(named("beforeHandle"))
            .and(takesArgument(0, named("org.restlet.data.Request")))
            .and(takesArgument(1, named("org.restlet.data.Response"))),
        this.getClass().getName() + "$RouteBeforeHandleAdvice");
  }

  @SuppressWarnings({"PrivateConstructorForUtilityClass", "unused"})
  public static class RouteBeforeHandleAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void getRouteInfo(@Advice.This Route route, @Advice.Argument(0) Request request) {
      String pattern = route.getTemplate().getPattern();

      HttpRouteHolder.updateHttpRoute(currentContext(), CONTROLLER, serverSpanName(), pattern);
    }
  }
}
