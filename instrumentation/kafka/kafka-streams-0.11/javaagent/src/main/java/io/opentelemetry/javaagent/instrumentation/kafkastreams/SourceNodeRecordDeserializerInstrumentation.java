/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.kafkastreams;

import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;

import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaConsumerContextUtil;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;

// This is necessary because SourceNodeRecordDeserializer drops the headers.  :-(
public class SourceNodeRecordDeserializerInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("org.apache.kafka.streams.processor.internals.SourceNodeRecordDeserializer");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod()
            .and(isPublic())
            .and(named("deserialize"))
            .and(takesArgument(0, named("org.apache.kafka.clients.consumer.ConsumerRecord")))
            .and(returns(named("org.apache.kafka.clients.consumer.ConsumerRecord"))),
        SourceNodeRecordDeserializerInstrumentation.class.getName() + "$SaveHeadersAdvice");
  }

  @SuppressWarnings("unused")
  public static class SaveHeadersAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void saveHeaders(
        @Advice.Argument(0) ConsumerRecord<?, ?> incoming,
        @Advice.Return(readOnly = false) ConsumerRecord<?, ?> result) {
      if (result == null) {
        return;
      }

      // copy headers from incoming to result
      for (Header header : incoming.headers()) {
        result.headers().add(header);
      }

      // copy the receive CONSUMER span association
      KafkaConsumerContextUtil.set(result, KafkaConsumerContextUtil.get(incoming));
    }
  }
}
