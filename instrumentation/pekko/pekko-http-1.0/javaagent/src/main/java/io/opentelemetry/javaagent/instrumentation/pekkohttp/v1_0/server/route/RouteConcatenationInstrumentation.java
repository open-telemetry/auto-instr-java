/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.pekkohttp.v1_0.server.route;

import static net.bytebuddy.matcher.ElementMatchers.named;

import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.pekko.http.scaladsl.server.RequestContext;
import org.apache.pekko.http.scaladsl.server.RouteResult;
import scala.concurrent.Future;

public class RouteConcatenationInstrumentation implements TypeInstrumentation {
  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("org.apache.pekko.http.scaladsl.server.RouteConcatenation$RouteWithConcatenation");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        named("$anonfun$$tilde$1"), this.getClass().getName() + "$ApplyAdvice");
    transformer.applyAdviceToMethod(
        named("$anonfun$$tilde$2"), this.getClass().getName() + "$Apply2Advice");
  }

  @SuppressWarnings("unused")
  public static class ApplyAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void onEnter() {
      // when routing dsl uses concat(path(...) {...}, path(...) {...}) we'll restore the currently
      // matched route after each matcher so that match attempts that failed wouldn't get recorded
      // in the route
      PekkoRouteHolder.save();
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void onExit(
        @Advice.Argument(value = 2) RequestContext ctx,
        @Advice.Return(readOnly = false) Future<RouteResult> fut) {
      fut = fut.andThen(new RestoreOnExit(), ctx.executionContext());
    }
  }

  @SuppressWarnings("unused")
  public static class Apply2Advice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void onEnter() {
      PekkoRouteHolder.reset();
    }
  }
}
