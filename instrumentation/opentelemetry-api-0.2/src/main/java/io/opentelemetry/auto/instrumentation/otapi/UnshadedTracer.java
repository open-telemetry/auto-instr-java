package io.opentelemetry.auto.instrumentation.otapi;

import lombok.extern.slf4j.Slf4j;
import unshaded.io.opentelemetry.context.NoopScope;
import unshaded.io.opentelemetry.context.Scope;
import unshaded.io.opentelemetry.context.propagation.BinaryFormat;
import unshaded.io.opentelemetry.context.propagation.HttpTextFormat;
import unshaded.io.opentelemetry.trace.Span;
import unshaded.io.opentelemetry.trace.SpanContext;
import unshaded.io.opentelemetry.trace.Tracer;

@Slf4j
public class UnshadedTracer implements Tracer {

  private final io.opentelemetry.trace.Tracer shadedTracer;

  public UnshadedTracer(final io.opentelemetry.trace.Tracer shadedTracer) {
    this.shadedTracer = shadedTracer;
  }

  @Override
  public Span getCurrentSpan() {
    return new UnshadedSpan(shadedTracer.getCurrentSpan());
  }

  @Override
  public Scope withSpan(final Span span) {
    if (span instanceof UnshadedSpan) {
      return new UnshadedScope(shadedTracer.withSpan(((UnshadedSpan) span).getShadedSpan()));
    } else {
      log.debug("unexpected span: {}", span);
      return NoopScope.getInstance();
    }
  }

  @Override
  public Span.Builder spanBuilder(final String spanName) {
    return new UnshadedSpanBuilder(shadedTracer.spanBuilder(spanName));
  }

  @Override
  public BinaryFormat<SpanContext> getBinaryFormat() {
    return new UnshadedBinaryFormat(shadedTracer.getBinaryFormat());
  }

  @Override
  public HttpTextFormat<SpanContext> getHttpTextFormat() {
    return new UnshadedHttpTextFormat(shadedTracer.getHttpTextFormat());
  }
}
