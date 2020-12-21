/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.liberty.dispatcher;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.instrumentation.api.tracer.HttpServerTracer;
import java.net.URI;
import java.net.URISyntaxException;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LibertyDispatcherTracer
    extends HttpServerTracer<
        LibertyRequestWrapper, LibertyResponseWrapper, LibertyConnectionWrapper, Void> {
  private static final Logger log = LoggerFactory.getLogger(LibertyDispatcherTracer.class);
  private static final LibertyDispatcherTracer TRACER = new LibertyDispatcherTracer();

  public static LibertyDispatcherTracer tracer() {
    return TRACER;
  }

  @Override
  protected String getInstrumentationName() {
    return "io.opentelemetry.javaagent.liberty-dispatcher";
  }

  @Override
  protected @Nullable Integer peerPort(LibertyConnectionWrapper libertyConnectionWrapper) {
    return libertyConnectionWrapper.peerPort();
  }

  @Override
  protected @Nullable String peerHostIP(LibertyConnectionWrapper libertyConnectionWrapper) {
    return libertyConnectionWrapper.peerHostIP();
  }

  @Override
  protected String flavor(
      LibertyConnectionWrapper libertyConnectionWrapper,
      LibertyRequestWrapper libertyRequestWrapper) {
    return libertyConnectionWrapper.getProtocol();
  }

  private static final TextMapPropagator.Getter<LibertyRequestWrapper> GETTER =
      new TextMapPropagator.Getter<LibertyRequestWrapper>() {

        @Override
        public Iterable<String> keys(LibertyRequestWrapper carrier) {
          return carrier.getAllHeaderNames();
        }

        @Override
        public String get(LibertyRequestWrapper carrier, String key) {
          return carrier.getHeaderValue(key);
        }
      };

  @Override
  protected TextMapPropagator.Getter<LibertyRequestWrapper> getGetter() {
    return GETTER;
  }

  @Override
  protected String url(LibertyRequestWrapper libertyRequestWrapper) {
    try {
      return new URI(
              libertyRequestWrapper.getScheme(),
              null,
              libertyRequestWrapper.getServerName(),
              libertyRequestWrapper.getServerPort(),
              libertyRequestWrapper.getRequestUri(),
              libertyRequestWrapper.getQueryString(),
              null)
          .toString();
    } catch (URISyntaxException e) {
      log.debug("Failed to construct request URI", e);
      return null;
    }
  }

  @Override
  protected String method(LibertyRequestWrapper libertyRequestWrapper) {
    return libertyRequestWrapper.getMethod();
  }

  @Override
  protected @Nullable String requestHeader(
      LibertyRequestWrapper libertyRequestWrapper, String name) {
    return libertyRequestWrapper.getHeaderValue(name);
  }

  @Override
  protected int responseStatus(LibertyResponseWrapper libertyResponseWrapper) {
    return libertyResponseWrapper.getStatus();
  }

  @Override
  public @Nullable Context getServerContext(Void none) {
    return null;
  }

  @Override
  protected void attachServerContext(Context context, Void none) {}
}
