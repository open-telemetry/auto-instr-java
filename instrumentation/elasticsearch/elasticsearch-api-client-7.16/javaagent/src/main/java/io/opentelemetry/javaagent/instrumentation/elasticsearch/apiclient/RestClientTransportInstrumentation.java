/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.elasticsearch.apiclient;

import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import co.elastic.clients.transport.Endpoint;
import io.opentelemetry.instrumentation.api.util.VirtualField;
import io.opentelemetry.instrumentation.elasticsearch.rest.common.v5_0.internal.ElasticsearchEndpointDefinition;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.elasticsearch.client.Request;

// up to 8.8 (included)
public class RestClientTransportInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("co.elastic.clients.transport.rest_client.RestClientTransport");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod()
            .and(named("prepareLowLevelRequest"))
            .and(takesArgument(1, named("co.elastic.clients.transport.Endpoint")))
            .and(returns(named("org.elasticsearch.client.Request"))),
        this.getClass().getName() + "$RestClientTransportAdvice");
  }

  @SuppressWarnings("unused")
  public static class RestClientTransportAdvice {

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void onPrepareLowLevelRequest(
        @Advice.Argument(1) Endpoint<?, ?, ?> endpoint, @Advice.Return Request request) {
      VirtualField<Request, ElasticsearchEndpointDefinition> virtualField =
          VirtualField.find(Request.class, ElasticsearchEndpointDefinition.class);
      String endpointId = endpoint.id();
      if (endpointId.startsWith("es/") && endpointId.length() > 3) {
        endpointId = endpointId.substring(3);
      }
      virtualField.set(request, ElasticsearchEndpointMap.get(endpointId));
    }
  }
}
