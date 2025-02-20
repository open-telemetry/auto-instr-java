/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.kafkastreams;

import static net.bytebuddy.matcher.ElementMatchers.isInterface;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isPackagePrivate;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.not;
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

// in 1.0.0 SourceNodeRecordDeserializer was refactored into RecordDeserializer
public class RecordDeserializerInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("org.apache.kafka.streams.processor.internals.RecordDeserializer")
        .and(not(isInterface()));
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod()
            .and(isPackagePrivate())
            .and(named("deserialize"))
            .and(takesArgument(1, named("org.apache.kafka.clients.consumer.ConsumerRecord")))
            .and(returns(named("org.apache.kafka.clients.consumer.ConsumerRecord"))),
        RecordDeserializerInstrumentation.class.getName() + "$DeserializeAdvice");
  }

  @SuppressWarnings("unused")
  public static class DeserializeAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onExit(
        @Advice.Argument(1) ConsumerRecord<?, ?> incoming,
        @Advice.Return(readOnly = false) ConsumerRecord<?, ?> result) {
      if (result == null) {
        return;
      }

      // on 1.x we need to copy headers from incoming to result
      if (!result.headers().iterator().hasNext()) {
        for (Header header : incoming.headers()) {
          result.headers().add(header);
        }
      }

      // copy the receive CONSUMER span association
      KafkaConsumerContextUtil.set(result, KafkaConsumerContextUtil.get(incoming));
    }
  }
}
