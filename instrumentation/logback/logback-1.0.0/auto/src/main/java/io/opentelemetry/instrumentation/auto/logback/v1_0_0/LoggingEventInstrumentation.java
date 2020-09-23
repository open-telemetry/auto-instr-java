/*
 * Copyright The OpenTelemetry Authors
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

package io.opentelemetry.instrumentation.auto.logback.v1_0_0;

import static io.opentelemetry.instrumentation.api.log.LoggingContextConstants.SAMPLED;
import static io.opentelemetry.instrumentation.api.log.LoggingContextConstants.SPAN_ID;
import static io.opentelemetry.instrumentation.api.log.LoggingContextConstants.TRACE_ID;
import static io.opentelemetry.javaagent.tooling.bytebuddy.matcher.AgentElementMatchers.implementsInterface;
import static java.util.Collections.singletonMap;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import ch.qos.logback.classic.spi.ILoggingEvent;
import com.google.auto.service.AutoService;
import io.opentelemetry.instrumentation.auto.api.InstrumentationContext;
import io.opentelemetry.instrumentation.logback.v1_0_0.internal.UnionMap;
import io.opentelemetry.javaagent.tooling.Instrumenter;
import io.opentelemetry.trace.Span;
import io.opentelemetry.trace.SpanContext;
import java.util.HashMap;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.implementation.bytecode.assign.Assigner.Typing;
import net.bytebuddy.matcher.ElementMatcher;

@AutoService(Instrumenter.class)
public class LoggingEventInstrumentation extends Instrumenter.Default {
  public LoggingEventInstrumentation() {
    super("logback");
  }

  @Override
  public String[] helperClassNames() {
    return new String[] {
      "io.opentelemetry.instrumentation.logback.v1_0_0.internal.UnionMap",
      "io.opentelemetry.instrumentation.logback.v1_0_0.internal.UnionMap$ConcatenatedSet",
      "io.opentelemetry.instrumentation.logback.v1_0_0.internal.UnionMap$ConcatenatedSet$ConcatenatedSetIterator"
    };
  }

  @Override
  public ElementMatcher<? super TypeDescription> typeMatcher() {
    return implementsInterface(named("ch.qos.logback.classic.spi.ILoggingEvent"));
  }

  @Override
  public Map<String, String> contextStore() {
    return singletonMap("ch.qos.logback.classic.spi.ILoggingEvent", Span.class.getName());
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    return singletonMap(
        isMethod()
            .and(isPublic())
            .and(named("getMDCPropertyMap").or(named("getMdc")))
            .and(takesArguments(0)),
        LoggingEventInstrumentation.class.getName() + "$GetMdcAdvice");
  }

  public static class GetMdcAdvice {
    @Advice.OnMethodExit(suppress = Throwable.class)
    public static void onExit(
        @Advice.This ILoggingEvent event,
        @Advice.Return(typing = Typing.DYNAMIC, readOnly = false) Map<String, String> contextData) {
      if (contextData != null && contextData.containsKey(TRACE_ID)) {
        // Assume already instrumented event if traceId is present.
        return;
      }

      Span currentSpan = InstrumentationContext.get(ILoggingEvent.class, Span.class).get(event);
      if (currentSpan == null || !currentSpan.getContext().isValid()) {
        return;
      }

      Map<String, String> spanContextData = new HashMap<>();
      SpanContext spanContext = currentSpan.getContext();
      spanContextData.put(TRACE_ID, spanContext.getTraceIdAsHexString());
      spanContextData.put(SPAN_ID, spanContext.getSpanIdAsHexString());
      if (spanContext.isSampled()) {
        spanContextData.put(SAMPLED, "true");
      }

      if (contextData == null) {
        contextData = spanContextData;
      } else {
        contextData = new UnionMap<>(contextData, spanContextData);
      }
    }
  }
}
