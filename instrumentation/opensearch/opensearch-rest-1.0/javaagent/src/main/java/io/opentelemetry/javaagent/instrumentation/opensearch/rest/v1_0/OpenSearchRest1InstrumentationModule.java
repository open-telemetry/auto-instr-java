/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.opensearch.rest.v1_0;

import static io.opentelemetry.javaagent.extension.matcher.AgentElementMatchers.hasClassesNamed;
import static java.util.Collections.singletonList;

import com.google.auto.service.AutoService;
import io.opentelemetry.javaagent.extension.instrumentation.InstrumentationModule;
import io.opentelemetry.javaagent.extension.instrumentation.TypeInstrumentation;
import java.util.List;
import net.bytebuddy.matcher.ElementMatcher;

@AutoService(InstrumentationModule.class)
public class OpenSearchRest1InstrumentationModule extends InstrumentationModule {
  public OpenSearchRest1InstrumentationModule() {
    super("opensearch-rest", "opensearch-rest-1.0", "opensearch");
  }

  @Override
  public ElementMatcher.Junction<ClassLoader> classLoaderMatcher() {
    // class introduced in 1.0.0
    return hasClassesNamed("org.opensearch.client.RestClient$InternalRequest");
  }

  @Override
  public List<TypeInstrumentation> typeInstrumentations() {
    return singletonList(new RestClientInstrumentation());
  }
}
