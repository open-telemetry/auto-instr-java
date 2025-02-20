/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.nacos.client.v2_0_0;

import static java.util.Arrays.asList;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.instrumentation.nacos.client.v2_0_0.instrumentations.GrpcConnectionInstrumentation;
import io.opentelemetry.javaagent.instrumentation.nacos.client.v2_0_0.instrumentations.RpcClientInstrumentation;
import io.opentelemetry.sdk.autoconfigure.spi.ConfigProperties;
import java.util.List;

@AutoService(InstrumentationModule.class)
public class NacosClientInstrumentationModule extends InstrumentationModule {
  public NacosClientInstrumentationModule() {
    super("nacos-client", "nacos-client-2.0.0");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return asList(new GrpcConnectionInstrumentation(), new RpcClientInstrumentation());
  }

  @Override
  public boolean defaultEnabled(ConfigProperties config) {
    return config.getBoolean(NacosClientConstants.OTEL_NACOS_CLIENT_ENABLED, false);
  }
}
