/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.joddhttp.v4_2;

import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.javaagent.bootstrap.internal.JavaagentHttpClientInstrumenterBuilder;
import java.util.Optional;
import jodd.http.HttpRequest;
import jodd.http.HttpResponse;

public final class JoddHttpSingletons {
  private static final String INSTRUMENTATION_NAME = "io.opentelemetry.jodd-http-4.2";

  private static final Instrumenter<HttpRequest, HttpResponse> INSTRUMENTER;

  static {
    INSTRUMENTER =
        JavaagentHttpClientInstrumenterBuilder.create(
            INSTRUMENTATION_NAME,
            new JoddHttpHttpAttributesGetter(),
            Optional.of(HttpHeaderSetter.INSTANCE));
  }

  public static Instrumenter<HttpRequest, HttpResponse> instrumenter() {
    return INSTRUMENTER;
  }

  private JoddHttpSingletons() {}
}
