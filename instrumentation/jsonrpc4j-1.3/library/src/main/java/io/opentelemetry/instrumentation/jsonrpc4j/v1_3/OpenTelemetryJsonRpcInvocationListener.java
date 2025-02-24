/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.jsonrpc4j.v1_3;

import com.fasterxml.jackson.databind.JsonNode;
import com.googlecode.jsonrpc4j.InvocationListener;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import java.lang.reflect.Method;
import java.util.List;

final class OpenTelemetryJsonRpcInvocationListener implements InvocationListener {

  static class JsonRpcContext {
    private final Context context;
    private final Scope scope;
    private final JsonRpcServerRequest request;

    JsonRpcContext(Context context, Scope scope, JsonRpcServerRequest request) {
      this.context = context;
      this.scope = scope;
      this.request = request;
    }

    Context getContext() {
      return context;
    }

    Scope getScope() {
      return scope;
    }

    JsonRpcServerRequest getRequest() {
      return request;
    }
  }

  private final Instrumenter<JsonRpcServerRequest, JsonRpcServerResponse> serverInstrumenter;

  private static final ThreadLocal<JsonRpcContext> threadLocalContext = new ThreadLocal<>();

  public OpenTelemetryJsonRpcInvocationListener(
      Instrumenter<JsonRpcServerRequest, JsonRpcServerResponse> serverInstrumenter) {
    this.serverInstrumenter = serverInstrumenter;
  }

  /**
   * This method will be invoked prior to a JSON-RPC service being invoked.
   *
   * @param method is the method that will be invoked.
   * @param arguments are the arguments that will be passed to the method when it is invoked.
   */
  @Override
  public void willInvoke(Method method, List<JsonNode> arguments) {
    Context parentContext = Context.current();
    JsonRpcServerRequest request = new JsonRpcServerRequest(method, arguments);
    if (!serverInstrumenter.shouldStart(parentContext, request)) {
      return;
    }

    Context context = serverInstrumenter.start(parentContext, request);
    Scope scope = context.makeCurrent();
    threadLocalContext.set(new JsonRpcContext(context, scope, request));
  }

  /**
   * This method will be invoked after a JSON-RPC service has been invoked.
   *
   * @param method is the method that will was invoked.
   * @param arguments are the arguments that were be passed to the method when it is invoked.
   * @param result is the result of the method invocation. If an error arose, this value will be
   *     null.
   * @param t is the throwable that was thrown from the invocation, if no error arose, this value
   *     will be null.
   * @param duration is approximately the number of milliseconds that elapsed during which the
   *     method was invoked.
   */
  @Override
  public void didInvoke(
      Method method, List<JsonNode> arguments, Object result, Throwable t, long duration) {
    JsonRpcServerResponse response = new JsonRpcServerResponse(method, arguments, result);
    threadLocalContext.get().getScope().close();
    serverInstrumenter.end(
        threadLocalContext.get().getContext(), threadLocalContext.get().getRequest(), response, t);
    threadLocalContext.remove();
  }
}
