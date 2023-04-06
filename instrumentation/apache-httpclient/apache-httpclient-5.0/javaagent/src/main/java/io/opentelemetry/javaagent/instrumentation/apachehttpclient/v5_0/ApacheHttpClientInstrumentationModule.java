/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.apachehttpclient.v5_0;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import java.util.ArrayList;
import java.util.List;

@AutoService(InstrumentationModule.class)
public final class ApacheHttpClientInstrumentationModule extends InstrumentationModule {

  public ApacheHttpClientInstrumentationModule() {
    super("apache-httpclient", "apache-httpclient-5.0");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    List<TypeInstrumentation> instrumentationList = new ArrayList<>();
    instrumentationList.add(new ApacheHttpClientInstrumentation());
    instrumentationList.add(new ApacheHttpAsyncClientInstrumentation());
    return instrumentationList;
  }
}
