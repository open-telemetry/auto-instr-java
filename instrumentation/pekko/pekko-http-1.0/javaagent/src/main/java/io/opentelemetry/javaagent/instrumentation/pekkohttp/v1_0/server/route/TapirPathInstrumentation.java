/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.pekkohttp.v1_0.server.route;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.pekko.http.scaladsl.server.RequestContext;
import org.apache.pekko.http.scaladsl.server.RouteResult;
import scala.Function1;
import scala.Function2;
import scala.Option;
import scala.PartialFunction;
import scala.Unit;
import scala.concurrent.Future;
import scala.util.Try;
import sttp.tapir.EndpointInput;
import sttp.tapir.server.ServerEndpoint;

public class TapirPathInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("sttp.tapir.server.pekkohttp.PekkoHttpServerInterpreter");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        named("toRoute").and(takesArgument(0, named("sttp.tapir.server.ServerEndpoint"))),
        this.getClass().getName() + "$ApplyAdvice");
  }

  public static class RouteWrapper implements Function1<RequestContext, Future<RouteResult>> {
    private final Function1<RequestContext, Future<RouteResult>> route;
    private final ServerEndpoint<?, ?> serverEndpoint;

    public RouteWrapper(
        ServerEndpoint<?, ?> serverEndpoint, Function1<RequestContext, Future<RouteResult>> route) {
      this.route = route;
      this.serverEndpoint = serverEndpoint;
    }

    public class Finalizer implements PartialFunction<Try<RouteResult>, Unit> {
      @Override
      public boolean isDefinedAt(Try<RouteResult> x) {
        return true;
      }

      @Override
      public Unit apply(Try<RouteResult> tryResult) {
        if (tryResult.isSuccess()) {
          RouteResult result = tryResult.get();
          if (result.getClass() == RouteResult.Complete.class) {
            String path =
                serverEndpoint.showPathTemplate(
                    (index, pc) ->
                        pc.name().isDefined()
                            ? "{" + pc.name().get() + "}"
                            : "{param" + index + "}",
                    Option.apply(
                        (Function2<Object, EndpointInput.Query<?>, String>)
                            (index, q) -> q.name() + "={" + q.name() + "}"),
                    true,
                    "*",
                    Option.apply("*"),
                    Option.apply("*"));

            PekkoRouteHolder.push(path);
            PekkoRouteHolder.endMatched();
          }
        }
        return null;
      }
    }

    @Override
    public Future<RouteResult> apply(RequestContext ctx) {
      return route.apply(ctx).andThen(new Finalizer(), ctx.executionContext());
    }
  }

  @SuppressWarnings("unused")
  public static class ApplyAdvice {
    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void onExit(
        @Advice.Argument(0) ServerEndpoint<?, ?> endpoint,
        @Advice.Return(readOnly = false) Function1<RequestContext, Future<RouteResult>> route) {
      route = new RouteWrapper(endpoint, route);
    }
  }
}
