/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.elasticsearch.transport.v5_3;

import static java.util.Collections.singletonList;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import java.util.List;

/** Beginning in version 5.3.0, DocumentRequest was renamed to DocWriteRequest. */
@AutoService(InstrumentationModule.class)
public class Elasticsearch53TransportClientInstrumentationModule extends InstrumentationModule {
  public Elasticsearch53TransportClientInstrumentationModule() {
    super("elasticsearch-transport", "elasticsearch-transport-5.3", "elasticsearch");
  }

  @Override
  public boolean isIndyModule() {
    // ExecuteAdvice uses both @Advice.Argument(readOnly = false) and @Advice.Local
    return false;
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return singletonList(new AbstractClientInstrumentation());
  }
}
