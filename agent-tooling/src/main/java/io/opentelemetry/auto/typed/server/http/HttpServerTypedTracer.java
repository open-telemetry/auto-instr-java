/*
 * Copyright 2020, OpenTelemetry Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.opentelemetry.auto.typed.server.http;

import io.opentelemetry.auto.typed.server.ServerTypedTracer;
import io.opentelemetry.context.propagation.HttpTextFormat;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.SpanContext;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class HttpServerTypedTracer<
        T extends HttpServerTypedSpan<T, REQUEST, RESPONSE>, REQUEST, RESPONSE>
    extends ServerTypedTracer<T, REQUEST, RESPONSE> {

  @Override
  protected Span.Builder buildSpan(final REQUEST request, final Span.Builder spanBuilder) {
    final SpanContext extract = tracer.getHttpTextFormat().extract(request, getGetter());
    if (extract.isValid()) {
      spanBuilder.setParent(extract);
    } else {
      // explicitly setting "no parent" in case a span was propagated to this thread
      // by the java-concurrent instrumentation when the thread was started
      spanBuilder.setNoParent();
    }
    return super.buildSpan(request, spanBuilder);
  }

  protected abstract HttpTextFormat.Getter<REQUEST> getGetter();
}
