/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.spring.autoconfigure.instrumentation.logging;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import io.opentelemetry.instrumentation.logback.appender.v1_0.OpenTelemetryAppender;
import java.util.Iterator;
import java.util.Optional;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;

class LogbackAppenderInstaller {

  static void install(ApplicationEnvironmentPreparedEvent applicationEnvironmentPreparedEvent) {
    Optional<OpenTelemetryAppender> existingOpenTelemetryAppender = findOpenTelemetryAppender();
    if (existingOpenTelemetryAppender.isPresent()) {
      reInitializeOpenTelemetryAppender(
          existingOpenTelemetryAppender, applicationEnvironmentPreparedEvent);
    } else if (isLogbackAppenderAddable(applicationEnvironmentPreparedEvent)) {
      addOpenTelemetryAppender(applicationEnvironmentPreparedEvent);
    }
  }

  private static boolean isLogbackAppenderAddable(
      ApplicationEnvironmentPreparedEvent applicationEnvironmentPreparedEvent) {
    boolean otelSdkDisableProperty =
        evaluateBooleanProperty(applicationEnvironmentPreparedEvent, "otel.sdk.disabled", false);
    boolean logbackInstrumentationEnabledProperty =
        evaluateBooleanProperty(
            applicationEnvironmentPreparedEvent,
            "otel.instrumentation.logback-appender.enabled",
            true);
    return !otelSdkDisableProperty && logbackInstrumentationEnabledProperty;
  }

  private static void reInitializeOpenTelemetryAppender(
      Optional<OpenTelemetryAppender> existingOpenTelemetryAppender,
      ApplicationEnvironmentPreparedEvent applicationEnvironmentPreparedEvent) {
    OpenTelemetryAppender openTelemetryAppender = existingOpenTelemetryAppender.get();
    // The OpenTelemetry appender is stopped and restarted from the
    // org.springframework.boot.context.logging.LoggingApplicationListener.initialize
    // method.
    // The OpenTelemetryAppender initializes the LoggingEventMapper in the start() method. So, here
    // we stop the OpenTelemetry appender before its re-initialization and its restart.
    openTelemetryAppender.stop();
    initializeOpenTelemetryAppenderFromProperties(
        applicationEnvironmentPreparedEvent, openTelemetryAppender);
    openTelemetryAppender.start();
  }

  private static void addOpenTelemetryAppender(
      ApplicationEnvironmentPreparedEvent applicationEnvironmentPreparedEvent) {
    ch.qos.logback.classic.Logger logger =
        (ch.qos.logback.classic.Logger)
            LoggerFactory.getILoggerFactory().getLogger(Logger.ROOT_LOGGER_NAME);
    OpenTelemetryAppender openTelemetryAppender = new OpenTelemetryAppender();
    initializeOpenTelemetryAppenderFromProperties(
        applicationEnvironmentPreparedEvent, openTelemetryAppender);
    openTelemetryAppender.start();
    logger.addAppender(openTelemetryAppender);
  }

  private static void initializeOpenTelemetryAppenderFromProperties(
      ApplicationEnvironmentPreparedEvent applicationEnvironmentPreparedEvent,
      OpenTelemetryAppender openTelemetryAppender) {

    // Implemented in the same way as the
    // org.springframework.boot.context.logging.LoggingApplicationListener, config properties not
    // available
    Boolean codeAttribute =
        evaluateBooleanProperty(
            applicationEnvironmentPreparedEvent,
            "otel.instrumentation.logback-appender.experimental.capture-code-attributes");
    if (codeAttribute != null) {
      openTelemetryAppender.setCaptureCodeAttributes(codeAttribute.booleanValue());
    }

    Boolean markerAttribute =
        evaluateBooleanProperty(
            applicationEnvironmentPreparedEvent,
            "otel.instrumentation.logback-appender.experimental.capture-marker-attribute");
    if (markerAttribute != null) {
      openTelemetryAppender.setCaptureMarkerAttribute(markerAttribute.booleanValue());
    }

    Boolean keyValuePairAttributes =
        evaluateBooleanProperty(
            applicationEnvironmentPreparedEvent,
            "otel.instrumentation.logback-appender.experimental.capture-key-value-pair-attributes");
    if (keyValuePairAttributes != null) {
      openTelemetryAppender.setCaptureKeyValuePairAttributes(keyValuePairAttributes.booleanValue());
    }

    Boolean logAttributes =
        evaluateBooleanProperty(
            applicationEnvironmentPreparedEvent,
            "otel.instrumentation.logback-appender.experimental-log-attributes");
    if (logAttributes != null) {
      openTelemetryAppender.setCaptureExperimentalAttributes(logAttributes.booleanValue());
    }

    Boolean loggerContextAttributes =
        evaluateBooleanProperty(
            applicationEnvironmentPreparedEvent,
            "otel.instrumentation.logback-appender.experimental.capture-logger-context-attributes");
    if (loggerContextAttributes != null) {
      openTelemetryAppender.setCaptureLoggerContext(loggerContextAttributes.booleanValue());
    }

    String mdcAttributeProperty =
        applicationEnvironmentPreparedEvent
            .getEnvironment()
            .getProperty(
                "otel.instrumentation.logback-appender.experimental.capture-mdc-attributes",
                String.class);
    if (mdcAttributeProperty != null) {
      openTelemetryAppender.setCaptureMdcAttributes(mdcAttributeProperty);
    }
  }

  private static Boolean evaluateBooleanProperty(
      ApplicationEnvironmentPreparedEvent applicationEnvironmentPreparedEvent, String property) {
    return applicationEnvironmentPreparedEvent
        .getEnvironment()
        .getProperty(property, Boolean.class);
  }

  private static boolean evaluateBooleanProperty(
      ApplicationEnvironmentPreparedEvent applicationEnvironmentPreparedEvent,
      String property,
      boolean defaultValue) {
    return applicationEnvironmentPreparedEvent
        .getEnvironment()
        .getProperty(property, Boolean.class, defaultValue);
  }

  private static Optional<OpenTelemetryAppender> findOpenTelemetryAppender() {
    ILoggerFactory loggerFactorySpi = LoggerFactory.getILoggerFactory();
    if (!(loggerFactorySpi instanceof LoggerContext)) {
      return Optional.empty();
    }
    LoggerContext loggerContext = (LoggerContext) loggerFactorySpi;
    for (ch.qos.logback.classic.Logger logger : loggerContext.getLoggerList()) {
      Iterator<Appender<ILoggingEvent>> appenderIterator = logger.iteratorForAppenders();
      while (appenderIterator.hasNext()) {
        Appender<ILoggingEvent> appender = appenderIterator.next();
        if (appender instanceof OpenTelemetryAppender) {
          OpenTelemetryAppender openTelemetryAppender = (OpenTelemetryAppender) appender;
          return Optional.of(openTelemetryAppender);
        }
      }
    }
    return Optional.empty();
  }

  private LogbackAppenderInstaller() {}
}
