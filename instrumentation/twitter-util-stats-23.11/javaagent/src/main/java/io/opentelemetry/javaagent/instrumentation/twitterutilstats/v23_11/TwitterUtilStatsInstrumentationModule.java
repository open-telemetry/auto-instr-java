/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.twitterutilstats.v23_11;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import java.util.Collections;
import java.util.List;

@AutoService(InstrumentationModule.class)
public class TwitterUtilStatsInstrumentationModule extends InstrumentationModule {

  public TwitterUtilStatsInstrumentationModule() {
    super("twitter-util-stats", "twitter-util-stats-23.11");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return Collections.singletonList(new LoadedStatsReceiverInstrumentation());
  }
}
