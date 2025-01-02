/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.api.instrumenter;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.instrumentation.api.internal.HttpRouteState;
import io.opentelemetry.instrumentation.api.internal.InstrumenterAccess;
import io.opentelemetry.instrumentation.api.internal.InstrumenterUtil;
import io.opentelemetry.instrumentation.api.internal.SupportabilityMetrics;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

/**
 * The {@link Instrumenter} encapsulates the entire logic for gathering telemetry, from collecting
 * the data, to starting and ending spans, to recording values using metrics instruments.
 *
 * <p>An {@link Instrumenter} is called at the start and the end of a request/response lifecycle.
 * When instrumenting a library, there will generally be four steps.
 *
 * <ul>
 *   <li>Create an {@link Instrumenter} using {@link InstrumenterBuilder}. Use the builder to
 *       configure any library-specific customizations, and also expose useful knobs to your user.
 *   <li>Call {@link Instrumenter#shouldStart(Context, Object)} and do not proceed if it returns
 *       {@code false}.
 *   <li>Call {@link Instrumenter#start(Context, Object)} at the beginning of a request.
 *   <li>Call {@link Instrumenter#end(Context, Object, Object, Throwable)} at the end of a request.
 * </ul>
 *
 * <p>For more detailed information about using the {@link Instrumenter} see the <a
 * href="https://github.com/open-telemetry/opentelemetry-java-instrumentation/blob/main/docs/contributing/using-instrumenter-api.md">Using
 * the Instrumenter API</a> page.
 */
public class Instrumenter<REQUEST, RESPONSE> {

  private static final ContextKey<OperationListener[]> START_OPERATION_LISTENERS =
      ContextKey.named("instrumenter-start-operation-listeners");

  /**
   * Returns a new {@link InstrumenterBuilder}.
   *
   * <p>The {@code instrumentationName} indicates the instrumentation library name, not the
   * instrument<b>ed</b> library name. The value passed in this parameter should uniquely identify
   * the instrumentation library so that during troubleshooting it's possible to determine where the
   * telemetry came from.
   *
   * <p>In OpenTelemetry instrumentations we use a convention to encode the minimum supported
   * version of the instrument<b>ed</b> library into the instrumentation name, for example {@code
   * io.opentelemetry.apache-httpclient-4.0}. This way, if there are different instrumentations for
   * different library versions it's easy to find out which instrumentations produced the telemetry
   * data.
   */
  public static <REQUEST, RESPONSE> InstrumenterBuilder<REQUEST, RESPONSE> builder(
      OpenTelemetry openTelemetry,
      String instrumentationName,
      SpanNameExtractor<? super REQUEST> spanNameExtractor) {
    return new InstrumenterBuilder<>(openTelemetry, instrumentationName, spanNameExtractor);
  }

  private static final SupportabilityMetrics supportability = SupportabilityMetrics.instance();

  private final String instrumentationName;
  private final Tracer tracer;
  private final SpanNameExtractor<? super REQUEST> spanNameExtractor;
  private final SpanKindExtractor<? super REQUEST> spanKindExtractor;
  private final SpanStatusExtractor<? super REQUEST, ? super RESPONSE> spanStatusExtractor;
  private final SpanLinksExtractor<? super REQUEST>[] spanLinksExtractors;
  private final AttributesExtractor<? super REQUEST, ? super RESPONSE>[] attributesExtractors;
  private final ContextCustomizer<? super REQUEST>[] contextCustomizers;
  private final OperationListener[] operationListeners;
  private final ErrorCauseExtractor errorCauseExtractor;
  private final boolean propagateOperationListenersToOnEnd;
  private final boolean enabled;
  private final SpanSuppressor spanSuppressor;

  @SuppressWarnings({"rawtypes", "unchecked"})
  Instrumenter(InstrumenterBuilder<REQUEST, RESPONSE> builder) {
    this.instrumentationName = builder.instrumentationName;
    this.tracer = builder.buildTracer();
    this.spanNameExtractor = builder.spanNameExtractor;
    this.spanKindExtractor = builder.spanKindExtractor;
    this.spanStatusExtractor = builder.spanStatusExtractor;
    this.spanLinksExtractors = builder.spanLinksExtractors.toArray(new SpanLinksExtractor[0]);
    this.attributesExtractors = builder.attributesExtractors.toArray(new AttributesExtractor[0]);
    this.contextCustomizers = builder.contextCustomizers.toArray(new ContextCustomizer[0]);
    this.operationListeners = builder.buildOperationListeners().toArray(new OperationListener[0]);
    this.errorCauseExtractor = builder.errorCauseExtractor;
    this.propagateOperationListenersToOnEnd = builder.propagateOperationListenersToOnEnd;
    this.enabled = builder.enabled;
    this.spanSuppressor = builder.buildSpanSuppressor();
  }

  /**
   * Determines whether the operation should be instrumented for telemetry or not. If the return
   * value is {@code true}, call {@link #start(Context, Object)} and {@link #end(Context, Object,
   * Object, Throwable)} around the instrumented operation; if the return value is false {@code
   * false} execute the operation directly without calling those methods.
   *
   * <p>The {@code parentContext} is the parent of the resulting instrumented operation and should
   * usually be {@link Context#current() Context.current()}. The {@code request} is the request
   * object of this operation.
   */
  public boolean shouldStart(Context parentContext, REQUEST request) {
    if (!enabled) {
      return false;
    }
    SpanKind spanKind = spanKindExtractor.extract(request);
    boolean suppressed = spanSuppressor.shouldSuppress(parentContext, spanKind);

    if (suppressed) {
      supportability.recordSuppressedSpan(spanKind, instrumentationName);
    }
    return !suppressed;
  }

  /**
   * Starts a new instrumented operation. The returned {@link Context} should be propagated along
   * with the operation and passed to the {@link #end(Context, Object, Object, Throwable)} method
   * when it is finished.
   *
   * <p>The {@code parentContext} is the parent of the resulting instrumented operation and should
   * usually be {@link Context#current() Context.current()}. The {@code request} is the request
   * object of this operation.
   */
  public Context start(Context parentContext, REQUEST request) {
    return doStart(parentContext, request, null);
  }

  /**
   * Ends an instrumented operation. It is of extreme importance for this method to be always called
   * after {@link #start(Context, Object) start()}. Calling {@code start()} without later {@code
   * end()} will result in inaccurate or wrong telemetry and context leaks.
   *
   * <p>The {@code context} must be the same value that was returned from {@link #start(Context,
   * Object)}. The {@code request} parameter is the request object of the operation, {@code
   * response} is the response object of the operation, and {@code error} is an exception that was
   * thrown by the operation or {@code null} if no error occurred.
   */
  public void end(
      Context context, REQUEST request, @Nullable RESPONSE response, @Nullable Throwable error) {
    doEnd(context, request, response, error, null);
  }

  /** Internal method for creating spans with given start/end timestamps. */
  Context startAndEnd(
      Context parentContext,
      REQUEST request,
      @Nullable RESPONSE response,
      @Nullable Throwable error,
      Instant startTime,
      Instant endTime) {
    Context context = doStart(parentContext, request, startTime);
    doEnd(context, request, response, error, endTime);
    return context;
  }

  private Context doStart(Context parentContext, REQUEST request, @Nullable Instant startTime) {
    SpanKind spanKind = spanKindExtractor.extract(request);
    SpanBuilder spanBuilder =
        tracer.spanBuilder(spanNameExtractor.extract(request)).setSpanKind(spanKind);

    if (startTime != null) {
      spanBuilder.setStartTimestamp(startTime);
    }

    SpanLinksBuilder spanLinksBuilder = new SpanLinksBuilderImpl(spanBuilder);
    for (SpanLinksExtractor<? super REQUEST> spanLinksExtractor : spanLinksExtractors) {
      spanLinksExtractor.extract(spanLinksBuilder, parentContext, request);
    }

    UnsafeAttributes attributes = new UnsafeAttributes();
    for (AttributesExtractor<? super REQUEST, ? super RESPONSE> extractor : attributesExtractors) {
      extractor.onStart(attributes, parentContext, request);
    }

    Context context = parentContext;

    // context customizers run before span start, so that they can have access to the parent span
    // context, and so that their additions to the context will be visible to span processors
    for (ContextCustomizer<? super REQUEST> contextCustomizer : contextCustomizers) {
      context = contextCustomizer.onStart(context, request, attributes);
    }

    boolean localRoot = LocalRootSpan.isLocalRoot(context);

    spanBuilder.setAllAttributes(attributes);
    Span span = spanBuilder.setParent(context).startSpan();
    context = context.with(span);

    if (operationListeners.length != 0) {
      // operation listeners run after span start, so that they have access to the current span
      // for capturing exemplars
      long startNanos = getNanos(startTime);
      for (int i = 0; i < operationListeners.length; i++) {
        context = operationListeners[i].onStart(context, attributes, startNanos);
      }
    }
    if (propagateOperationListenersToOnEnd || context.get(START_OPERATION_LISTENERS) != null) {
      // when start and end are not called on the same instrumenter we need to use the operation
      // listeners that were used during start in end to correctly handle metrics like
      // http.server.active_requests that is recorded both in start and end
      //
      // need to also add when there is already START_OPERATION_LISTENERS, otherwise this
      // instrumenter will call its parent's operation listeners in doEnd
      context = context.with(START_OPERATION_LISTENERS, operationListeners);
    }

    if (localRoot) {
      context = LocalRootSpan.store(context, span);
      if (spanKind == SpanKind.SERVER) {
        HttpRouteState.updateSpan(context, span);
      }
    }

    return spanSuppressor.storeInContext(context, spanKind, span);
  }

  private void doEnd(
      Context context,
      REQUEST request,
      @Nullable RESPONSE response,
      @Nullable Throwable error,
      @Nullable Instant endTime) {
    Span span = Span.fromContext(context);

    if (error != null) {
      error = errorCauseExtractor.extract(error);
      span.recordException(error);
    }

    UnsafeAttributes attributes = new UnsafeAttributes();
    for (AttributesExtractor<? super REQUEST, ? super RESPONSE> extractor : attributesExtractors) {
      extractor.onEnd(attributes, context, request, response, error);
    }
    span.setAllAttributes(attributes);

    OperationListener[] operationListeners = context.get(START_OPERATION_LISTENERS);
    if (operationListeners == null) {
      operationListeners = this.operationListeners;
    }
    if (operationListeners.length != 0) {
      long endNanos = getNanos(endTime);
      for (int i = operationListeners.length - 1; i >= 0; i--) {

        operationListeners[i].onEnd(context, attributes, endNanos);
      }
    }

    SpanStatusBuilder spanStatusBuilder = new SpanStatusBuilderImpl(span);
    spanStatusExtractor.extract(spanStatusBuilder, request, response, error);

    if (endTime != null) {
      span.end(endTime);
    } else {
      span.end();
    }
  }

  private static long getNanos(@Nullable Instant time) {
    if (time == null) {
      return System.nanoTime();
    }
    return TimeUnit.SECONDS.toNanos(time.getEpochSecond()) + time.getNano();
  }

  static {
    InstrumenterUtil.setInstrumenterAccess(
        new InstrumenterAccess() {
          @Override
          public <RQ, RS> Context startAndEnd(
              Instrumenter<RQ, RS> instrumenter,
              Context parentContext,
              RQ request,
              @Nullable RS response,
              @Nullable Throwable error,
              Instant startTime,
              Instant endTime) {
            return instrumenter.startAndEnd(
                parentContext, request, response, error, startTime, endTime);
          }

          @Override
          public <REQUEST, RESPONSE> Context suppressSpan(
              Instrumenter<REQUEST, RESPONSE> instrumenter,
              Context parentContext,
              REQUEST request) {
            SpanKind spanKind = instrumenter.spanKindExtractor.extract(request);

            return instrumenter.spanSuppressor.storeInContext(
                parentContext, spanKind, Span.getInvalid());
          }
        });
  }
}
