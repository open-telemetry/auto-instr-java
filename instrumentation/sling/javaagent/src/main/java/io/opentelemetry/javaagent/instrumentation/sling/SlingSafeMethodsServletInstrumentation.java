/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.sling;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.implementsInterface;
import static io.opentelemetry.javaagent.instrumentation.sling.SlingSingletons.REQUEST_ATTR_RESOLVED_SERVLET_NAME;
import static io.opentelemetry.javaagent.instrumentation.sling.SlingSingletons.helper;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.semconv.http.HttpServerRoute;
import io.opentelemetry.instrumentation.api.semconv.http.HttpServerRouteSource;
import io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import javax.servlet.ServletRequest;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.sling.api.SlingHttpServletRequest;

public class SlingSafeMethodsServletInstrumentation implements TypeInstrumentation {
  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return implementsInterface(named("javax.servlet.Servlet"));
  }

  @Override
  public ElementMatcher<ClassLoader> classLoaderOptimization() {
    return hasClassesNamed("org.apache.sling.api.SlingHttpServletRequest");
  }

  @Override
  public void transform(TypeTransformer transformer) {

    String adviceClassName = this.getClass().getName() + "$ServiceServletAdvice";
    transformer.applyAdviceToMethod(
        named("service")
            .and(takesArguments(2))
            .and(takesArgument(0, named("javax.servlet.ServletRequest")))
            .and(takesArgument(1, named("javax.servlet.ServletResponse"))),
        adviceClassName);
  }

  @SuppressWarnings("unused")
  public static class ServiceServletAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void onEnter(
        @Advice.Argument(0) ServletRequest request,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope) {

      if (!(request instanceof SlingHttpServletRequest)) {
        return;
      }

      SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;

      Context parentContext = Java8BytecodeBridge.currentContext();

      if (!helper().shouldStart(parentContext, slingRequest)) {
        return;
      }

      // written by ServletResolverInstrumentation
      Object servletName = request.getAttribute(REQUEST_ATTR_RESOLVED_SERVLET_NAME);
      if (!(servletName instanceof String)) {
        return;
      }

      // TODO - figure out why don't we have matches for all requests and find a better way to
      // filter
      context = helper().start(parentContext, slingRequest);
      scope = context.makeCurrent();

      // ensure that the top-level route is Sling-specific
      HttpServerRoute.update(context, HttpServerRouteSource.CONTROLLER, (String) servletName);

      // cleanup and ensure we don't have reuse the resolved Servlet name by accident for other
      // requests
      request.removeAttribute(REQUEST_ATTR_RESOLVED_SERVLET_NAME);
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void onExit(
        @Advice.Argument(0) ServletRequest request,
        @Advice.Thrown Throwable throwable,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope) {

      if (scope == null) {
        return;
      }
      scope.close();

      SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
      helper().end(context, slingRequest, null, throwable);
    }
  }
}
