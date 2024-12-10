/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.apachehttpclient.v5_2;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.semconv.http.HttpClientRequestResendCount;
import java.io.IOException;
import org.apache.hc.client5.http.classic.ExecChain;
import org.apache.hc.client5.http.classic.ExecChain.Scope;
import org.apache.hc.client5.http.classic.ExecChainHandler;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.HttpResponse;

class OtelExecChainHandler implements ExecChainHandler {

  private static final String REQUEST_PARENT_CONTEXT_ATTRIBUTE_ID =
      OtelExecChainHandler.class.getName() + ".context";

  private final Instrumenter<ApacheHttpClientRequest, HttpResponse> instrumenter;
  private final ContextPropagators propagators;

  public OtelExecChainHandler(
      Instrumenter<ApacheHttpClientRequest, HttpResponse> instrumenter,
      ContextPropagators propagators) {
    this.instrumenter = instrumenter;
    this.propagators = propagators;
  }

  @Override
  public ClassicHttpResponse execute(ClassicHttpRequest request, Scope scope, ExecChain chain)
      throws IOException, HttpException {
    Context parentContext =
        scope.clientContext.getAttribute(REQUEST_PARENT_CONTEXT_ATTRIBUTE_ID, Context.class);
    request.setVersion(scope.clientContext.getProtocolVersion());
    if (parentContext == null) {
      parentContext = HttpClientRequestResendCount.initialize(Context.current());
      scope.clientContext.setAttribute(REQUEST_PARENT_CONTEXT_ATTRIBUTE_ID, parentContext);
    }

    ApacheHttpClientRequest instrumenterRequest = getApacheHttpClientRequest(request, scope);

    if (!instrumenter.shouldStart(parentContext, instrumenterRequest)) {
      return chain.proceed(request, scope);
    }

    Context context = instrumenter.start(parentContext, instrumenterRequest);
    propagators.getTextMapPropagator().inject(context, request, HttpHeaderSetter.INSTANCE);

    return execute(request, instrumenterRequest, chain, scope, context);
  }

  private ClassicHttpResponse execute(
      ClassicHttpRequest request,
      ApacheHttpClientRequest instrumenterRequest,
      ExecChain chain,
      Scope scope,
      Context context)
      throws IOException, HttpException {
    ClassicHttpResponse response = null;
    Throwable error = null;
    try (io.opentelemetry.context.Scope ignored = context.makeCurrent()) {
      response = chain.proceed(request, scope);
      return response;
    } catch (Exception e) {
      error = e;
      throw e;
    } finally {
      instrumenter.end(context, instrumenterRequest, response, error);
    }
  }

  private static ApacheHttpClientRequest getApacheHttpClientRequest(
      ClassicHttpRequest request, Scope scope) {
    HttpHost host = null;
    if (scope.route.getTargetHost() != null) {
      host = scope.route.getTargetHost();
    } else if (scope.clientContext.getHttpRoute().getTargetHost() != null) {
      host = scope.clientContext.getHttpRoute().getTargetHost();
    }
    if (host != null
        && ((host.getSchemeName().equals("https") && host.getPort() == 443)
            || (host.getSchemeName().equals("http") && host.getPort() == 80))) {
      // port seems to be added to the host by route planning for standard ports even if not
      // specified in the URL. There doesn't seem to be a way to differentiate between explicit
      // and implicit port, but ignore in both cases to match the more common case.
      host = new HttpHost(host.getSchemeName(), host.getHostName(), -1);
    }

    return new ApacheHttpClientRequest(host, request);
  }
}
