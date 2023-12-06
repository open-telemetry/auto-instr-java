/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.servlet.common.response;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.instrumentation.api.incubator.semconv.code.CodeAttributeGetter;
import io.opentelemetry.instrumentation.api.incubator.semconv.code.CodeAttributesExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.code.CodeSpanNameExtractor;
import io.opentelemetry.instrumentation.api.incubator.semconv.util.ClassAndMethod;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;

public final class ResponseInstrumenterFactory {

  public static Instrumenter<ClassAndMethod, Void> createInstrumenter(String instrumentationName) {
    CodeAttributeGetter<ClassAndMethod> codeAttributeGetter = ClassAndMethod.codeAttributeGetter();
    return Instrumenter.<ClassAndMethod, Void>builder(
            GlobalOpenTelemetry.get(),
            instrumentationName,
            CodeSpanNameExtractor.create(codeAttributeGetter))
        .addAttributesExtractor(CodeAttributesExtractor.create(codeAttributeGetter))
        .buildInstrumenter();
  }

  private ResponseInstrumenterFactory() {}
}
