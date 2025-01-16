/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.aerospike;

import static io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge.currentContext;
import static io.opentelemetry.javaagent.instrumentation.aerospike.AerospikeSingletons.instrumenter;
import static io.opentelemetry.javaagent.instrumentation.aerospike.internal.CustomElementMatcher.iterableHasAtLeastOne;
import static net.bytebuddy.matcher.ElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
import static net.bytebuddy.matcher.ElementMatchers.takesGenericArguments;

import com.aerospike.client.Key;
import io.opentelemetry.context.Context;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import io.opentelemetry.javaagent.instrumentation.aerospike.internal.AerospikeRequest;
import io.opentelemetry.javaagent.instrumentation.aerospike.internal.AerospikeRequestContext;
import java.util.Locale;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class AerospikeClientInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return hasSuperType(named("com.aerospike.client.IAerospikeClient"));
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod()
            .and(takesGenericArguments(iterableHasAtLeastOne(named("com.aerospike.client.Key"))))
            .and(
                not(
                    takesGenericArguments(
                        iterableHasAtLeastOne(named("com.aerospike.client.async.EventLoop"))))),
        this.getClass().getName() + "$SyncCommandAdvice");

    transformer.applyAdviceToMethod(
        isMethod()
            .and(takesGenericArguments(iterableHasAtLeastOne(named("com.aerospike.client.Key"))))
            .and(
                takesGenericArguments(
                    iterableHasAtLeastOne(named("com.aerospike.client.async.EventLoop")))),
        this.getClass().getName() + "$AsyncCommandAdvice");
  }

  @SuppressWarnings("unused")
  public static class SyncCommandAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void startInstrumentation(
        @Advice.Origin("#m") String methodName,
        @Advice.AllArguments Object[] args,
        @Advice.Local("AerospikeContext") AerospikeRequestContext requestContext) {
      Key key = null;
      for (Object object : args) {
        if (object instanceof Key) {
          key = (Key) object;
        }
      }
      if (key == null) {
        return;
      }
      Context parentContext = currentContext();
      AerospikeRequest request =
          AerospikeRequest.create(methodName.toUpperCase(Locale.ROOT), key.namespace, key.setName);
      if (!instrumenter().shouldStart(parentContext, request)) {
        return;
      }
      Context context = instrumenter().start(parentContext, request);
      requestContext = AerospikeRequestContext.attach(request, context);
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void stopInstrumentation(
        @Advice.Thrown Throwable ex,
        @Advice.Local("AerospikeContext") AerospikeRequestContext requestContext) {
      requestContext.setThrowable(ex);
      requestContext.detachAndEnd();
    }
  }

  @SuppressWarnings("unused")
  public static class AsyncCommandAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void startInstrumentation(
        @Advice.Origin("#m") String methodName,
        @Advice.AllArguments Object[] args,
        @Advice.Local("AerospikeContext") AerospikeRequestContext requestContext) {
      Key key = null;
      for (Object object : args) {
        if (object instanceof Key) {
          key = (Key) object;
        }
      }
      if (key == null) {
        return;
      }
      Context parentContext = currentContext();
      AerospikeRequest request =
          AerospikeRequest.create(methodName.toUpperCase(Locale.ROOT), key.namespace, key.setName);
      if (!instrumenter().shouldStart(parentContext, request)) {
        return;
      }
      Context context = instrumenter().start(parentContext, request);
      requestContext = AerospikeRequestContext.attach(request, context);
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void stopInstrumentaionIfError(
        @Advice.Thrown Throwable ex,
        @Advice.Local("AerospikeContext") AerospikeRequestContext requestContext) {
      if (ex != null) {
        requestContext.setThrowable(ex);
        requestContext.detachAndEnd();
      }
    }
  }
}
