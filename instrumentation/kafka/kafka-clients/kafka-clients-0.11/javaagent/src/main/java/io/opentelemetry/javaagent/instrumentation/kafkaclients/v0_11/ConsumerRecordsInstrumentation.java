/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.kafkaclients.v0_11;

import static io.opentelemetry.javaagent.bootstrap.kafka.KafkaClientsConsumerProcessTracing.wrappingEnabledSupplier;
import static io.opentelemetry.javaagent.instrumentation.kafkaclients.v0_11.KafkaSingletons.consumerProcessInstrumenter;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaConsumerContext;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaConsumerContextUtil;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.TracingIterable;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.TracingIterator;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.TracingList;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.util.Iterator;
import java.util.List;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;

public class ConsumerRecordsInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("org.apache.kafka.clients.consumer.ConsumerRecords");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod()
            .and(isPublic())
            .and(named("records"))
            .and(takesArgument(0, String.class))
            .and(returns(Iterable.class)),
        ConsumerRecordsInstrumentation.class.getName() + "$IterableAdvice");
    transformer.applyAdviceToMethod(
        isMethod()
            .and(isPublic())
            .and(named("records"))
            .and(takesArgument(0, named("org.apache.kafka.common.TopicPartition")))
            .and(returns(List.class)),
        ConsumerRecordsInstrumentation.class.getName() + "$ListAdvice");
    transformer.applyAdviceToMethod(
        isMethod()
            .and(isPublic())
            .and(named("iterator"))
            .and(takesArguments(0))
            .and(returns(Iterator.class)),
        ConsumerRecordsInstrumentation.class.getName() + "$IteratorAdvice");
  }

  @SuppressWarnings("unused")
  public static class IterableAdvice {

    @SuppressWarnings("unchecked")
    @Advice.OnMethodExit(suppress = Throwable.class)
    public static <K, V> void wrap(
        @Advice.This ConsumerRecords<?, ?> records,
        @Advice.Return(readOnly = false) Iterable<ConsumerRecord<K, V>> iterable) {

      // it's important not to suppress consumer span creation here because this instrumentation can
      // leak the context and so there may be a leaked consumer span in the context, in which
      // case it's important to overwrite the leaked span instead of suppressing the correct span
      // (https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/1947)
      KafkaConsumerContext consumerContext = KafkaConsumerContextUtil.get(records);
      iterable =
          TracingIterable.wrap(
              iterable, consumerProcessInstrumenter(), wrappingEnabledSupplier(), consumerContext);
    }
  }

  @SuppressWarnings("unused")
  public static class ListAdvice {

    @SuppressWarnings("unchecked")
    @Advice.OnMethodExit(suppress = Throwable.class)
    public static <K, V> void wrap(
        @Advice.This ConsumerRecords<?, ?> records,
        @Advice.Return(readOnly = false) List<ConsumerRecord<K, V>> list) {

      // it's important not to suppress consumer span creation here because this instrumentation can
      // leak the context and so there may be a leaked consumer span in the context, in which
      // case it's important to overwrite the leaked span instead of suppressing the correct span
      // (https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/1947)
      KafkaConsumerContext consumerContext = KafkaConsumerContextUtil.get(records);
      list =
          TracingList.wrap(
              list, consumerProcessInstrumenter(), wrappingEnabledSupplier(), consumerContext);
    }
  }

  @SuppressWarnings("unused")
  public static class IteratorAdvice {

    @SuppressWarnings("unchecked")
    @Advice.OnMethodExit(suppress = Throwable.class)
    public static <K, V> void wrap(
        @Advice.This ConsumerRecords<?, ?> records,
        @Advice.Return(readOnly = false) Iterator<ConsumerRecord<K, V>> iterator) {

      // it's important not to suppress consumer span creation here because this instrumentation can
      // leak the context and so there may be a leaked consumer span in the context, in which
      // case it's important to overwrite the leaked span instead of suppressing the correct span
      // (https://github.com/open-telemetry/opentelemetry-java-instrumentation/issues/1947)
      KafkaConsumerContext consumerContext = KafkaConsumerContextUtil.get(records);
      iterator =
          TracingIterator.wrap(
              iterator, consumerProcessInstrumenter(), wrappingEnabledSupplier(), consumerContext);
    }
  }
}
