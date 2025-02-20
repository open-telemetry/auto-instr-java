/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.elasticsearch.rest.v7_0;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.elasticsearch.rest.common.v5_0.internal.ElasticsearchRestRequest;
import io.opentelemetry.instrumentation.elasticsearch.rest.common.v5_0.internal.RestResponseListener;
import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.InvocationHandlerAdapter;
import net.bytebuddy.matcher.ElementMatchers;
import org.apache.http.Header;
import org.elasticsearch.client.Node;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseListener;
import org.elasticsearch.client.RestClient;

class RestClientWrapper {
  private static final Class<?> proxyClass = createProxyClass();
  private static final Field targetField = getTargetField(proxyClass);
  private static final Field instrumenterSupplierField = getInstrumenterSupplierField(proxyClass);
  private static final Function<RestClient, RestClient> proxyFactory = getProxyFactory(proxyClass);

  private static Class<?> createProxyClass() {
    return new ByteBuddy()
        .subclass(RestClient.class)
        .defineField("target", RestClient.class, Visibility.PUBLIC)
        // using Supplier instead of Instrumenter in case RestClientWrapper and opentelemetry apis
        // are in a child class loader of RestClient's class loader and Instrumenter is not visible
        // for RestClient
        .defineField("instrumenterSupplier", Supplier.class, Visibility.PUBLIC)
        .method(ElementMatchers.any())
        .intercept(
            InvocationHandlerAdapter.of(
                (proxy, method, args) -> {
                  RestClient target = (RestClient) targetField.get(proxy);
                  Instrumenter<ElasticsearchRestRequest, Response> instrumenter =
                      getInstrumenter(proxy);
                  // target is null when running proxy constructor
                  if (target == null || instrumenter == null) {
                    return null;
                  }

                  // instrument performRequest and performRequestAsync methods
                  if ("performRequest".equals(method.getName())
                      && args.length == 1
                      && args[0] instanceof Request
                      && Response.class == method.getReturnType()) {
                    Request request = (Request) args[0];
                    Context parentContext = Context.current();
                    ElasticsearchRestRequest otelRequest =
                        ElasticsearchRestRequest.create(request.getMethod(), request.getEndpoint());
                    if (!instrumenter.shouldStart(parentContext, otelRequest)) {
                      return method.invoke(target, args);
                    }

                    Context context = instrumenter.start(parentContext, otelRequest);

                    Response response;
                    try (Scope ignored = context.makeCurrent()) {
                      response = (Response) method.invoke(target, args);
                    } catch (Throwable t) {
                      instrumenter.end(context, otelRequest, null, t);
                      throw t;
                    }
                    instrumenter.end(context, otelRequest, response, null);
                    return response;

                  } else if ("performRequestAsync".equals(method.getName())
                      && args.length == 2
                      && args[0] instanceof Request
                      && args[1] instanceof ResponseListener) {

                    Request request = (Request) args[0];
                    ResponseListener responseListener = (ResponseListener) args[1];
                    Context parentContext = Context.current();
                    ElasticsearchRestRequest otelRequest =
                        ElasticsearchRestRequest.create(request.getMethod(), request.getEndpoint());
                    if (!instrumenter.shouldStart(parentContext, otelRequest)) {
                      return method.invoke(target, args);
                    }

                    Context context = instrumenter.start(parentContext, otelRequest);
                    args[1] =
                        new RestResponseListener(
                            responseListener, parentContext, instrumenter, context, otelRequest);
                    try (Scope ignored = context.makeCurrent()) {
                      return method.invoke(target, args);
                    } catch (Throwable t) {
                      instrumenter.end(context, otelRequest, null, t);
                      throw t;
                    }
                    // span ended in RestResponseListener
                  }

                  // delegate to wrapped RestClient
                  return method.invoke(target, args);
                }))
        .make()
        .load(RestClient.class.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
        .getLoaded();
  }

  private static Field getTargetField(Class<?> clazz) {
    return getProxyField(clazz, "target");
  }

  private static Field getInstrumenterSupplierField(Class<?> clazz) {
    return getProxyField(clazz, "instrumenterSupplier");
  }

  private static Field getProxyField(Class<?> clazz, String fieldName) {
    try {
      return clazz.getDeclaredField(fieldName);
    } catch (NoSuchFieldException exception) {
      throw new IllegalStateException("Could not find proxy field", exception);
    }
  }

  @SuppressWarnings("unchecked")
  private static Instrumenter<ElasticsearchRestRequest, Response> getInstrumenter(Object proxy)
      throws IllegalAccessException {
    Supplier<Instrumenter<ElasticsearchRestRequest, Response>> supplier =
        (Supplier<Instrumenter<ElasticsearchRestRequest, Response>>)
            instrumenterSupplierField.get(proxy);
    return supplier != null ? supplier.get() : null;
  }

  private static Function<RestClient, RestClient> getProxyFactory(Class<?> clazz) {
    for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
      Class<?>[] parameterTypes = constructor.getParameterTypes();
      if (parameterTypes.length >= 3
          && !parameterTypes[0].isPrimitive()
          && parameterTypes[1] == Header[].class
          && parameterTypes[2] == List.class) {
        return restClient -> {
          List<Node> nodes = restClient.getNodes();
          // all the proxy methods will delegate to the wrapped RestClient, we need to fill only the
          // arguments that are required by the constructor
          Object[] arguments = new Object[parameterTypes.length];
          arguments[1] = new Header[0];
          arguments[2] = nodes;
          for (int i = 3; i < parameterTypes.length; i++) {
            if (parameterTypes[i].isPrimitive()) {
              arguments[i] = getDefaultValue(parameterTypes[i]);
            }
          }
          try {
            return (RestClient) constructor.newInstance(arguments);
          } catch (Exception exception) {
            throw new IllegalStateException("Failed to construct proxy instance", exception);
          }
        };
      }
    }
    throw new IllegalStateException("Failed to find suitable constructor");
  }

  // create a single element array of given type, this method is used to get the default value of
  // a primitive type
  @SuppressWarnings("unchecked")
  private static <T> T getDefaultValue(Class<T> clazz) {
    return (T) Array.get(Array.newInstance(clazz, 1), 0);
  }

  static RestClient wrap(
      RestClient restClient, Instrumenter<ElasticsearchRestRequest, Response> instrumenter) {
    RestClient wrapped = proxyFactory.apply(restClient);
    try {
      // set wrapped RestClient instance and the instrumenter on the proxy
      targetField.set(wrapped, restClient);
      instrumenterSupplierField.set(
          wrapped, (Supplier<Instrumenter<ElasticsearchRestRequest, Response>>) () -> instrumenter);
      return wrapped;
    } catch (Exception exception) {
      throw new IllegalStateException("Failed to construct proxy instance", exception);
    }
  }

  private RestClientWrapper() {}
}
