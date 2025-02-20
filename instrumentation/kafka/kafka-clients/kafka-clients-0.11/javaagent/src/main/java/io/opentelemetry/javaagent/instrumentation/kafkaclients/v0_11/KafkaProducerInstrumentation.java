/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.kafkaclients.v0_11;

import static io.opentelemetry.javaagent.instrumentation.kafkaclients.v0_11.KafkaSingletons.producerInstrumenter;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaProducerRequest;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaPropagation;
import io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.kafka.clients.ApiVersions;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.ProducerRecord;

public class KafkaProducerInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("org.apache.kafka.clients.producer.KafkaProducer");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod()
            .and(isPublic())
            .and(named("send"))
            .and(takesArgument(0, named("org.apache.kafka.clients.producer.ProducerRecord")))
            .and(takesArgument(1, named("org.apache.kafka.clients.producer.Callback"))),
        KafkaProducerInstrumentation.class.getName() + "$SendAdvice");
  }

  @SuppressWarnings("unused")
  public static class SendAdvice {

    @Advice.OnMethodEnter(suppress = Throwable.class)
    public static KafkaProducerRequest onEnter(
        @Advice.FieldValue("apiVersions") ApiVersions apiVersions,
        @Advice.FieldValue("clientId") String clientId,
        @Advice.Argument(value = 0, readOnly = false) ProducerRecord<?, ?> record,
        @Advice.Argument(value = 1, readOnly = false) Callback callback,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope) {

      KafkaProducerRequest request = KafkaProducerRequest.create(record, clientId);
      Context parentContext = Java8BytecodeBridge.currentContext();
      if (!producerInstrumenter().shouldStart(parentContext, request)) {
        return null;
      }

      context = producerInstrumenter().start(parentContext, request);
      scope = context.makeCurrent();

      if (KafkaSingletons.isProducerPropagationEnabled()
          && KafkaPropagation.shouldPropagate(apiVersions)) {
        record = KafkaPropagation.propagateContext(context, record);
      }

      callback = new ProducerCallback(callback, parentContext, context, request);
      return request;
    }

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void stopSpan(
        @Advice.Enter KafkaProducerRequest request,
        @Advice.Thrown Throwable throwable,
        @Advice.Local("otelContext") Context context,
        @Advice.Local("otelScope") Scope scope) {
      if (scope == null) {
        return;
      }
      scope.close();

      if (throwable != null) {
        producerInstrumenter().end(context, request, null, throwable);
      }
      // span finished by ProducerCallback
    }
  }
}
