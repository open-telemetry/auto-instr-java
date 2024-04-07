/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.pulsar.v2_8;

import static io.opentelemetry.javaagent.instrumentation.pulsar.v2_8.telemetry.PulsarSingletons.producerInstrumenter;
import static net.bytebuddy.matcher.ElementMatchers.hasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.isConstructor;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import io.opentelemetry.javaagent.instrumentation.pulsar.v2_8.telemetry.PulsarRequest;
import java.util.concurrent.CompletableFuture;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.impl.MessageImpl;
import org.apache.pulsar.client.impl.ProducerImpl;
import org.apache.pulsar.client.impl.PulsarClientImpl;
import org.apache.pulsar.client.impl.SendCallback;

public class ProducerImplInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("org.apache.pulsar.client.impl.ProducerImpl");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isConstructor()
            .and(isPublic())
            .and(
                takesArgument(0, hasSuperType(named("org.apache.pulsar.client.api.PulsarClient")))),
        ProducerImplInstrumentation.class.getName() + "$ProducerImplConstructorAdvice");

    transformer.applyAdviceToMethod(
        isMethod()
            .and(named("sendAsync"))
            .and(takesArgument(1, named("org.apache.pulsar.client.impl.SendCallback"))),
        ProducerImplInstrumentation.class.getName() + "$ProducerSendAsyncMethodAdvice");

    transformer.applyAdviceToMethod(
        isMethod().and(isPublic()).and(named("closeAsync")),
        ProducerImplInstrumentation.class.getName() + "$ProducerCloseAsyncMethodAdvice");
  }

  @SuppressWarnings("unused")
  public static class ProducerImplConstructorAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void intercept(
        @Advice.This ProducerImpl<?> producer, @Advice.Argument(value = 0) PulsarClient client) {
      PulsarClientImpl pulsarClient = (PulsarClientImpl) client;
      String brokerUrl = pulsarClient.getLookup().getServiceUrl();
      String topic = producer.getTopic();
      VirtualFieldStore.inject(producer, brokerUrl, topic);
      PulsarMetricsRegistry.getMetricsRegistry().registerProducer(producer);
    }
  }

  @SuppressWarnings("unused")
  public static class ProducerSendAsyncMethodAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void before(
        @Advice.This ProducerImpl<?> producer,
        @Advice.Argument(value = 0) Message<?> message,
        @Advice.Argument(value = 1, readOnly = false) SendCallback callback) {
      Context parent = Context.current();
      PulsarRequest request = PulsarRequest.create(message, VirtualFieldStore.extract(producer));

      if (!producerInstrumenter().shouldStart(parent, request)) {
        return;
      }

      Context context = producerInstrumenter().start(parent, request);
      callback = new SendCallbackWrapper(context, request, callback);
    }
  }

  public static class ProducerCloseAsyncMethodAdvice {
    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static void before(@Advice.This ProducerImpl<?> producer) {
      PulsarMetricsRegistry.getMetricsRegistry().deleteProducer(producer);
    }
  }

  public static class SendCallbackWrapper implements SendCallback {

    private final Context context;
    private final PulsarRequest request;
    private final SendCallback delegate;

    public SendCallbackWrapper(Context context, PulsarRequest request, SendCallback callback) {
      this.context = context;
      this.request = request;
      this.delegate = callback;
    }

    @Override
    public void sendComplete(Exception e) {
      if (context == null) {
        this.delegate.sendComplete(e);
        return;
      }

      try (Scope ignore = context.makeCurrent()) {
        this.delegate.sendComplete(e);
      } finally {
        producerInstrumenter().end(context, request, null, e);
      }
    }

    @Override
    public void addCallback(MessageImpl<?> msg, SendCallback scb) {
      this.delegate.addCallback(msg, scb);
    }

    @Override
    public SendCallback getNextSendCallback() {
      return this.delegate.getNextSendCallback();
    }

    @Override
    public MessageImpl<?> getNextMessage() {
      return this.delegate.getNextMessage();
    }

    @Override
    public CompletableFuture<MessageId> getFuture() {
      return this.delegate.getFuture();
    }
  }
}
