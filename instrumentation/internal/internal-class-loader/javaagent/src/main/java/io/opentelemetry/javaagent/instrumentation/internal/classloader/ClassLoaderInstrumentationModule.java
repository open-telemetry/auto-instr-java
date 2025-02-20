/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.internal.classloader;

import static java.util.Arrays.asList;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.extension.instrumentation.internal.ExperimentalInstrumentationModule;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.util.Arrays;
import java.util.List;

@AutoService(InstrumentationModule.class)
public class ClassLoaderInstrumentationModule extends InstrumentationModule
    implements ExperimentalInstrumentationModule {
  public ClassLoaderInstrumentationModule() {
    super("internal-class-loader");
  }

  @Override
  public boolean defaultEnabled(ConfigProperties config) {
    // internal instrumentations are always enabled by default
    return true;
  }

  @Override
  public List<String> getAdditionalHelperClassNames() {
    return Arrays.asList(
        "io.opentelemetry.javaagent.instrumentation.internal.classloader.BootstrapPackagesHelper",
        "io.opentelemetry.javaagent.tooling.Constants");
  }

  @Override
  public List<String> injectedClassNames() {
    return getAdditionalHelperClassNames();
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return asList(
        new BootDelegationInstrumentation(),
        new LoadInjectedClassInstrumentation(),
        new ResourceInjectionInstrumentation(),
        new DefineClassInstrumentation());
  }
}
