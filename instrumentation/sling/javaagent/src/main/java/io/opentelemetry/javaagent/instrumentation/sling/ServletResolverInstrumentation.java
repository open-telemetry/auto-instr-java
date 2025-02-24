/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.sling;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.implementsInterface;
import static io.opentelemetry.javaagent.instrumentation.sling.SlingSingletons.REQUEST_ATTR_RESOLVED_SERVLET_NAME;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import javax.servlet.Servlet;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.sling.api.SlingHttpServletRequest;

public class ServletResolverInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return implementsInterface(named("org.apache.sling.api.servlets.ServletResolver"));
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod()
            .and(named("resolveServlet"))
            .and(takesArguments(1))
            .and(takesArgument(0, named("org.apache.sling.api.SlingHttpServletRequest"))),
        this.getClass().getName() + "$ResolveServletAdvice");
  }

  @SuppressWarnings("unused")
  public static class ResolveServletAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void onExit(
        @Advice.Argument(0) SlingHttpServletRequest request,
        @Advice.Return Servlet servlet,
        @Advice.Thrown Throwable throwable,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope) {

      String name = null;

      if (servlet.getServletConfig() != null) {
        name = servlet.getServletConfig().getServletName();
      }
      if (name == null || name.isEmpty()) {
        name = servlet.getServletInfo();
      }
      if (name == null || name.isEmpty()) {
        name = servlet.getClass().getName();
      }

      request.setAttribute(REQUEST_ATTR_RESOLVED_SERVLET_NAME, name);
    }
  }
}
