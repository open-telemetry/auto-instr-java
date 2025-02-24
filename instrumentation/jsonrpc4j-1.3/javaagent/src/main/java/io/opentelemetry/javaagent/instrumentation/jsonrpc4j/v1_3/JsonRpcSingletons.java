/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.jsonrpc4j.v1_3;

import com.googlecode.jsonrpc4j.IJsonRpcClient;
import com.googlecode.jsonrpc4j.InvocationListener;
import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.jsonrpc4j.v1_3.JsonRpcServerTelemetry;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

public final class JsonRpcSingletons {

  public static final InvocationListener SERVER_INVOCATION_LISTENER;

  public static final Instrumenter<JsonRpcClientRequest, JsonRpcClientResponse> CLIENT_INSTRUMENTER;

  static {
    JsonRpcServerTelemetry serverTelemetry =
        JsonRpcServerTelemetry.builder(GlobalOpenTelemetry.get()).build();
    JsonRpcClientTelemetry clientTelemetry =
        JsonRpcClientTelemetry.builder(GlobalOpenTelemetry.get()).build();

    SERVER_INVOCATION_LISTENER = serverTelemetry.newServerInvocationListener();
    CLIENT_INSTRUMENTER = clientTelemetry.getClientInstrumenter();
  }

  @SuppressWarnings({"unchecked"})
  public static <T> T instrumentCreateClientProxy(
      ClassLoader classLoader,
      Class<T> proxyInterface,
      IJsonRpcClient client,
      Map<String, String> extraHeaders,
      Object proxy) {

    return (T)
        Proxy.newProxyInstance(
            classLoader,
            new Class<?>[] {proxyInterface},
            new InvocationHandler() {
              @Override
              public Object invoke(Object proxy1, Method method, Object[] args) throws Throwable {
                // before invoke
                Context parentContext = Context.current();
                JsonRpcClientRequest request = new JsonRpcClientRequest(method, args);
                if (!CLIENT_INSTRUMENTER.shouldStart(parentContext, request)) {
                  try {
                    return method.invoke(proxy, args);
                  } catch (InvocationTargetException exception) {
                    throw exception.getCause();
                  }
                }

                Context context = CLIENT_INSTRUMENTER.start(parentContext, request);
                Object result;
                try (Scope scope = context.makeCurrent()) {
                  result = method.invoke(proxy, args);
                } catch (Throwable t) {
                  // after invoke
                  CLIENT_INSTRUMENTER.end(context, request, null, t);
                  throw t;
                }
                CLIENT_INSTRUMENTER.end(
                    context,
                    new JsonRpcClientRequest(method, args),
                    new JsonRpcClientResponse(result),
                    null);
                return result;
              }
            });
  }

  private JsonRpcSingletons() {}
}
