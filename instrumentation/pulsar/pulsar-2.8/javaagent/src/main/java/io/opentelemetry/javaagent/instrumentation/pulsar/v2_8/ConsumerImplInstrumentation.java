/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.pulsar.v2_8;

import static io.opentelemetry.javaagent.instrumentation.pulsar.v2_8.telemetry.PulsarSingletons.startAndEndConsumerReceive;
import static io.opentelemetry.javaagent.instrumentation.pulsar.v2_8.telemetry.PulsarSingletons.wrap;
import static io.opentelemetry.javaagent.instrumentation.pulsar.v2_8.telemetry.PulsarSingletons.wrapBatch;
import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isProtected;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.namedOneOf;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.internal.Timer;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.util.concurrent.CompletableFuture;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.pulsar.client.api.Consumer;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.Messages;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.impl.ConsumerImpl;
import org.apache.pulsar.client.impl.PulsarClientImpl;

public class ConsumerImplInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return namedOneOf(
        "org.apache.pulsar.client.impl.ConsumerImpl",
        "org.apache.pulsar.client.impl.MultiTopicsConsumerImpl");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    String className = ConsumerImplInstrumentation.class.getName();

    transformer.applyAdviceToMethod(isConstructor(), className + "$ConsumerConstructorAdvice");

    // internalReceive will apply to Consumer#receive(long,TimeUnit)
    // and called before MessageListener#receive.
    transformer.applyAdviceToMethod(
        isMethod()
            .and(isProtected())
            .and(named("internalReceive"))
            .and(takesArguments(2))
            .and(takesArgument(1, named("java.util.concurrent.TimeUnit"))),
        className + "$ConsumerInternalReceiveAdvice");
    // internalReceive will apply to Consumer#receive()
    transformer.applyAdviceToMethod(
        isMethod().and(isProtected()).and(named("internalReceive")).and(takesArguments(0)),
        className + "$ConsumerSyncReceiveAdvice");
    // internalReceiveAsync will apply to Consumer#receiveAsync()
    transformer.applyAdviceToMethod(
        isMethod().and(isProtected()).and(named("internalReceiveAsync")).and(takesArguments(0)),
        className + "$ConsumerAsyncReceiveAdvice");
    // internalBatchReceiveAsync will apply to Consumer#batchReceive() and
    // Consumer#batchReceiveAsync()
    transformer.applyAdviceToMethod(
        isMethod()
            .and(isProtected())
            .and(named("internalBatchReceiveAsync"))
            .and(takesArguments(0)),
        className + "$ConsumerBatchAsyncReceiveAdvice");
    transformer.applyAdviceToMethod(
        isMethod().and(isPublic()).and(named("closeAsync")),
        className + "$ConsumerCloseAsyncMethodAdvice");
  }

  @SuppressWarnings("unused")
  public static class ConsumerConstructorAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void after(
        @Advice.This Consumer<?> consumer, @Advice.Argument(value = 0) PulsarClient client) {

      PulsarClientImpl pulsarClient = (PulsarClientImpl) client;
      String url = pulsarClient.getLookup().getServiceUrl();
      VirtualFieldStore.inject(consumer, url);
      if (consumer instanceof ConsumerImpl) {
        // Skip the MultiTopicsConsumerImpl to avoid duplicated metrics since the
        // MultiTopicsConsumerImpl instance will contain a couple of internal ConsumerImpl instance
        PulsarMetricsRegistry.getMetricsRegistry().registerConsumer((ConsumerImpl<?>) consumer);
      }
    }
  }

  @SuppressWarnings("unused")
  public static class ConsumerInternalReceiveAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static Timer before() {
      return Timer.start();
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void after(
        @Advice.Enter Timer timer,
        @Advice.This Consumer<?> consumer,
        @Advice.Return Message<?> message,
        @Advice.Thrown Throwable throwable) {
      Context parent = Context.current();
      Context current = startAndEndConsumerReceive(parent, message, timer, consumer, throwable);
      if (current != null && throwable == null) {
        // ConsumerBase#internalReceive(long,TimeUnit) will be called before
        // ConsumerListener#receive(Consumer,Message), so, need to inject Context into Message.
        VirtualFieldStore.inject(message, current);
      }
    }
  }

  @SuppressWarnings("unused")
  public static class ConsumerSyncReceiveAdvice {

    @Advice.OnMethodEnter
    public static Timer before() {
      return Timer.start();
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void after(
        @Advice.Enter Timer timer,
        @Advice.This Consumer<?> consumer,
        @Advice.Return Message<?> message,
        @Advice.Thrown Throwable throwable) {
      Context parent = Context.current();
      startAndEndConsumerReceive(parent, message, timer, consumer, throwable);
      // No need to inject context to message.
    }
  }

  @SuppressWarnings("unused")
  public static class ConsumerAsyncReceiveAdvice {

    @Advice.OnMethodEnter
    public static Timer before() {
      return Timer.start();
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void after(
        @Advice.Enter Timer timer,
        @Advice.This Consumer<?> consumer,
        @Advice.Return(readOnly = false) CompletableFuture<Message<?>> future) {
      future = wrap(future, timer, consumer);
    }
  }

  @SuppressWarnings("unused")
  public static class ConsumerBatchAsyncReceiveAdvice {

    @Advice.OnMethodEnter
    public static Timer before() {
      return Timer.start();
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void after(
        @Advice.Enter Timer timer,
        @Advice.This Consumer<?> consumer,
        @Advice.Return(readOnly = false) CompletableFuture<Messages<?>> future) {
      future = wrapBatch(future, timer, consumer);
    }
  }

  public static class ConsumerCloseAsyncMethodAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void before(@Advice.This Consumer<?> consumer) {
      if (consumer instanceof ConsumerImpl<?>) {
        PulsarMetricsRegistry.getMetricsRegistry().deleteConsumer((ConsumerImpl<?>) consumer);
      }
    }
  }
}
