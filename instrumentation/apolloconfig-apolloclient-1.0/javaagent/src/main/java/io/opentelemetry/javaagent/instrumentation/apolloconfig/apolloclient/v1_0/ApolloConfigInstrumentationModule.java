/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apolloconfig.apolloclient.v1_0;

import static java.util.Collections.singletonList;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.util.List;

@AutoService(InstrumentationModule.class)
public class ApolloConfigInstrumentationModule extends InstrumentationModule {
  public ApolloConfigInstrumentationModule() {
    super("apolloconfig-apolloclient", "apolloconfig-apolloclient-1.0");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return singletonList(new ApolloRepositoryChangeInstrumentation());
  }

  @Override
  public boolean defaultEnabled(ConfigProperties config) {
    return false;
  }
}
