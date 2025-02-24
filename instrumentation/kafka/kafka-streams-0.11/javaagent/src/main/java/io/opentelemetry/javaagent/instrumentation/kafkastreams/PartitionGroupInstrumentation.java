/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.kafkastreams;

import static io.opentelemetry.javaagent.bootstrap.Java8BytecodeBridge.currentContext;
import static io.opentelemetry.javaagent.instrumentation.kafkastreams.KafkaStreamsSingletons.instrumenter;
import static io.opentelemetry.javaagent.instrumentation.kafkastreams.StateHolder.HOLDER;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isPackagePrivate;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.returns;

import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaConsumerContext;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaConsumerContextUtil;
import io.opentelemetry.instrumentation.kafkaclients.common.v0_11.internal.KafkaProcessRequest;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;
import org.apache.kafka.streams.processor.internals.StampedRecord;

// the advice applied by this instrumentation actually starts the span
public class PartitionGroupInstrumentation implements TypeInstrumentation {

  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    return named("org.apache.kafka.streams.processor.internals.PartitionGroup");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    transformer.applyAdviceToMethod(
        isMethod()
            .and(isPackagePrivate())
            .and(named("nextRecord"))
            .and(returns(named("org.apache.kafka.streams.processor.internals.StampedRecord"))),
        PartitionGroupInstrumentation.class.getName() + "$NextRecordAdvice");
  }

  @SuppressWarnings("unused")
  public static class NextRecordAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onExit(@Advice.Return StampedRecord record) {
      if (record == null) {
        return;
      }

      StateHolder holder = HOLDER.get();
      if (holder == null) {
        // somehow nextRecord() was called outside of process()
        return;
      }

      KafkaConsumerContext consumerContext = KafkaConsumerContextUtil.get(record.value);
      Context receiveContext = consumerContext.getContext();

      // use the receive CONSUMER span as parent if it's available
      Context parentContext = receiveContext != null ? receiveContext : currentContext();
      KafkaProcessRequest request = KafkaProcessRequest.create(consumerContext, record.value);

      if (!instrumenter().shouldStart(parentContext, request)) {
        return;
      }
      Context context = instrumenter().start(parentContext, request);
      holder.set(request, context, context.makeCurrent());
    }
  }
}
