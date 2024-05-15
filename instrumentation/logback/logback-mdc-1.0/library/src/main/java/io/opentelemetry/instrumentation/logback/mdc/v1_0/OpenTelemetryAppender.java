/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.logback.mdc.v1_0;

import static io.opentelemetry.instrumentation.api.incubator.log.LoggingContextConstants.SPAN_ID;
import static io.opentelemetry.instrumentation.api.incubator.log.LoggingContextConstants.TRACE_FLAGS;
import static io.opentelemetry.instrumentation.api.incubator.log.LoggingContextConstants.TRACE_ID;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggerContextVO;
import ch.qos.logback.classic.spi.LoggingEventVO;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import ch.qos.logback.core.spi.AppenderAttachable;
import ch.qos.logback.core.spi.AppenderAttachableImpl;
import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.logback.mdc.v1_0.internal.UnionMap;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class OpenTelemetryAppender extends UnsynchronizedAppenderBase<ILoggingEvent>
    implements AppenderAttachable<ILoggingEvent> {
  private boolean addBaggage;

  private final AppenderAttachableImpl<ILoggingEvent> aai = new AppenderAttachableImpl<>();

  private String traceIdAttributeName = TRACE_ID;
  private String spanIdAttributeName = SPAN_ID;
  private String traceFlagsAttributeName = TRACE_FLAGS;

  /**
   * When set to true this will enable addition of all baggage entries to MDC. This can be done by
   * adding the following to the logback.xml config for this appender. {@code
   * <addBaggage>true</addBaggage>}
   *
   * @param addBaggage True if baggage should be added to MDC
   */
  public void setAddBaggage(boolean addBaggage) {
    this.addBaggage = addBaggage;
  }

  public ILoggingEvent wrapEvent(ILoggingEvent event) {
    Map<String, String> eventContext = event.getMDCPropertyMap();
    if (eventContext != null && eventContext.containsKey(TRACE_ID)) {
      // Assume already instrumented event if traceId is present.
      return event;
    }

    Map<String, String> contextData = new HashMap<>();
    Context context = Context.current();
    Span currentSpan = Span.fromContext(context);

    if (currentSpan.getSpanContext().isValid()) {
      SpanContext spanContext = currentSpan.getSpanContext();
      contextData.put(traceIdAttributeName, spanContext.getTraceId());
      contextData.put(spanIdAttributeName, spanContext.getSpanId());
      contextData.put(traceFlagsAttributeName, spanContext.getTraceFlags().asHex());
    }

    if (addBaggage) {
      Baggage baggage = Baggage.fromContext(context);
      baggage.forEach(
          (key, value) ->
              contextData.put(
                  // prefix all baggage values to avoid clashes with existing context
                  "baggage." + key, value.getValue()));
    }

    if (eventContext == null) {
      eventContext = contextData;
    } else {
      eventContext = new UnionMap<>(eventContext, contextData);
    }
    Map<String, String> eventContextMap = eventContext;
    LoggerContextVO oldVo = event.getLoggerContextVO();
    LoggerContextVO vo =
        oldVo != null
            ? new LoggerContextVO(oldVo.getName(), eventContextMap, oldVo.getBirthTime())
            : null;

    ILoggingEvent wrappedEvent =
        (ILoggingEvent)
            Proxy.newProxyInstance(
                ILoggingEvent.class.getClassLoader(),
                new Class<?>[] {ILoggingEvent.class},
                (proxy, method, args) -> {
                  if ("getMDCPropertyMap".equals(method.getName())) {
                    return eventContextMap;
                  } else if ("getLoggerContextVO".equals(method.getName())) {
                    return vo;
                  }
                  try {
                    return method.invoke(event, args);
                  } catch (InvocationTargetException exception) {
                    throw exception.getCause();
                  }
                });
    // https://github.com/qos-ch/logback/blob/9e833ec858953a2296afdc3292f8542fc08f2a45/logback-classic/src/main/java/ch/qos/logback/classic/net/LoggingEventPreSerializationTransformer.java#L29
    // LoggingEventPreSerializationTransformer accepts only subclasses of LoggingEvent and
    // LoggingEventVO, here we transform our wrapped event into a LoggingEventVO
    return LoggingEventVO.build(wrappedEvent);
  }

  @Override
  protected void append(ILoggingEvent event) {
    aai.appendLoopOnAppenders(wrapEvent(event));
  }

  @Override
  public void addAppender(Appender<ILoggingEvent> appender) {
    aai.addAppender(appender);
  }

  @Override
  public Iterator<Appender<ILoggingEvent>> iteratorForAppenders() {
    return aai.iteratorForAppenders();
  }

  @Override
  public Appender<ILoggingEvent> getAppender(String name) {
    return aai.getAppender(name);
  }

  @Override
  public boolean isAttached(Appender<ILoggingEvent> appender) {
    return aai.isAttached(appender);
  }

  @Override
  public void detachAndStopAllAppenders() {
    aai.detachAndStopAllAppenders();
  }

  @Override
  public boolean detachAppender(Appender<ILoggingEvent> appender) {
    return aai.detachAppender(appender);
  }

  @Override
  public boolean detachAppender(String name) {
    return aai.detachAppender(name);
  }

  public void setTraceIdAttributeName(String attributeName) {
    this.traceIdAttributeName = attributeName;
  }

  public void setSpanIdAttributeName(String attributeName) {
    this.spanIdAttributeName = attributeName;
  }

  public void setTraceFlagsAttributeName(String attributeName) {
    this.traceFlagsAttributeName = attributeName;
  }
}
