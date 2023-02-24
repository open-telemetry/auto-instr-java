/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpasyncclient;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import io.opentelemetry.javaagent.instrumentation.apachehttpclient.v4_0.commons.ApacheHttpClientProcessorInstrumentation;
import java.util.ArrayList;
import java.util.List;

@AutoService(InstrumentationModule.class)
public final class ApacheHttpAsyncClientInstrumentationModule extends InstrumentationModule {
  public ApacheHttpAsyncClientInstrumentationModule() {
    super("apache-httpasyncclient", "apache-httpasyncclient-4.1");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    List<TypeInstrumentation> instrumentationList = new ArrayList<>();
    instrumentationList.add(new ApacheHttpAsyncClientInstrumentation());
    instrumentationList.add(new ApacheHttpClientProcessorInstrumentation());
    return instrumentationList;
  }
}
