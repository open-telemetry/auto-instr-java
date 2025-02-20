/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.twitterutilstats.v23_11;

import static net.bytebuddy.matcher.ElementMatchers.isTypeInitializer;
import static net.bytebuddy.matcher.ElementMatchers.named;

import com.twitter.finagle.stats.BroadcastStatsReceiver;
import com.twitter.finagle.stats.StatsReceiver;
import io.opentelemetry.javaagent.bootstrap.internal.AgentInstrumentationConfig;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.TypeTransformer;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

public class LoadedStatsReceiverInstrumentation implements TypeInstrumentation {
  @Override
  public ElementMatcher<TypeDescription> typeMatcher() {
    // $ => we want the scala object, not the class
    return named("com.twitter.finagle.stats.LoadedStatsReceiver$");
  }

  @Override
  public void transform(TypeTransformer transformer) {
    // perform this on the class initializer bc LoadedStatsReceiver::self is a var, not a val;
    // we don't want to wrap this every time, and we need to respect behavior when someone sets it
    transformer.applyAdviceToMethod(
        isTypeInitializer(), LoadedStatsReceiverInstrumentation.class.getName() + "$ClinitAdvice");
  }

  @SuppressWarnings("unused")
  public static class ClinitAdvice {

    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void methodEnter(
        @Advice.FieldValue(value = "self", readOnly = false) StatsReceiver self) {
      String mode =
          AgentInstrumentationConfig.get()
              .getString("otel.instrumentation.twitter-util-stats.metrics.mode", "additive");
      List<StatsReceiver> sr;
      if (Objects.equals(mode, "additive")) {
        sr = Arrays.asList(new OtelStatsReceiverProxy(), self);

      } else {
        sr = Collections.singletonList(new OtelStatsReceiverProxy());
      }
      // emulate the original invocation to avoid downstream side effects;
      // iow make no assumptions about how BroadcastStatsReceiver behaves
      self =
          BroadcastStatsReceiver.apply(
              scala.jdk.CollectionConverters.ListHasAsScala(sr).asScala().toSeq());
    }
  }
}
