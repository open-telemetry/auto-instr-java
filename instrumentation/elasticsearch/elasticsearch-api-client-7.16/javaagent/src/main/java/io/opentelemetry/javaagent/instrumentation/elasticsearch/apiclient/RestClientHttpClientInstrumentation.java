/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.elasticsearch.apiclient;

import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.namedOneOf;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.util.VirtualField;
import io.opentelemetry.instrumentation.elasticsearch.rest.common.v5_0.internal.ElasticsearchEndpointDefinition;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.elasticsearch.client.Request;

// starting from 8.9
public class RestClientHttpClientInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("co.elastic.clients.transport.rest_client.RestClientHttpClient");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod()
            .and(namedOneOf("performRequest", "performRequestAsync"))
            .and(takesArgument(0, String.class)),
        this.getClass().getName() + "$PerformRequestAdvice");
    transformer.applyAdviceToMethod(
        isMethod()
            .and(named("createRestRequest"))
            .and(returns(named("org.elasticsearch.client.Request"))),
        this.getClass().getName() + "$CreateRestRequestAdvice");
  }

  @SuppressWarnings("unused")
  public static class PerformRequestAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static Scope onEnter(@Advice.Argument(0) String endpointId) {
      return EndpointId.storeInContext(Context.current(), endpointId).makeCurrent();
    }

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void onExit(@Advice.Enter Scope scope) {
      if (scope != null) {
        scope.close();
      }
    }
  }

  @SuppressWarnings("unused")
  public static class CreateRestRequestAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class, onThrowable = Throwable.class)
    public static void onExit(@Advice.Return Request request) {
      String endpointId = EndpointId.get(Context.current());
      if (endpointId == null) {
        return;
      }
      if (endpointId.startsWith("es/") && endpointId.length() > 3) {
        endpointId = endpointId.substring(3);
      }
      VirtualField.find(Request.class, ElasticsearchEndpointDefinition.class)
          .set(request, ElasticsearchEndpointMap.get(endpointId));
    }
  }
}
