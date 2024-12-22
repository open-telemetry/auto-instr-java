/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.spring.webmvc.v5_3;

import static io.opentelemetry.instrumentation.api.semconv.http.HttpServerRouteSource.CONTROLLER;
import static java.util.Objects.requireNonNull;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.api.semconv.http.HttpServerRoute;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.web.filter.OncePerRequestFilter;

final class WebMvcTelemetryProducingFilter extends OncePerRequestFilter implements Ordered {

  private final Instrumenter<HttpServletRequest, HttpServletResponse> instrumenter;
  private final HttpRouteSupport httpRouteSupport = new HttpRouteSupport();

  WebMvcTelemetryProducingFilter(
      Instrumenter<HttpServletRequest, HttpServletResponse> instrumenter) {
    this.instrumenter = instrumenter;
  }

  @Override
  public void afterPropertiesSet() {
    // don't do anything, in particular do not call initFilterBean()
  }

  @Override
  protected void initFilterBean() {
    // FilterConfig must be non-null at this point
    httpRouteSupport.onFilterInit(requireNonNull(getFilterConfig()));
  }

  @Override
  public void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    Context parentContext = Context.current();
    if (!instrumenter.shouldStart(parentContext, request)) {
      filterChain.doFilter(request, response);
      return;
    }

    Context context = instrumenter.start(parentContext, request);
    if (httpRouteSupport.hasMappings()) {
      HttpServerRoute.update(context, CONTROLLER, httpRouteSupport::getHttpRoute, request);
    }
    AsyncAwareHttpServletRequest asyncAwareRequest =
        new AsyncAwareHttpServletRequest(request, response, context);

    try (Scope ignored = context.makeCurrent()) {
      filterChain.doFilter(asyncAwareRequest, response);
    } catch (Throwable t) {
      instrumenter.end(context, request, response, t);
      throw t;
    }
    if (asyncAwareRequest.isNotAsync()) {
      instrumenter.end(context, request, response, null);
    }
  }

  @Override
  public void destroy() {}

  @Override
  public int getOrder() {
    // Run after all HIGHEST_PRECEDENCE items
    return Ordered.HIGHEST_PRECEDENCE + 1;
  }

  private class AsyncAwareHttpServletRequest extends HttpServletRequestWrapper {
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final Context context;
    private final AtomicBoolean listenerAttached = new AtomicBoolean();

    AsyncAwareHttpServletRequest(
        HttpServletRequest request, HttpServletResponse response, Context context) {
      super(request);
      this.request = request;
      this.response = response;
      this.context = context;
    }

    @Override
    public AsyncContext startAsync() {
      AsyncContext asyncContext = super.startAsync();
      attachListener(asyncContext);
      return asyncContext;
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) {
      AsyncContext asyncContext = super.startAsync(servletRequest, servletResponse);
      attachListener(asyncContext);
      return asyncContext;
    }

    private void attachListener(AsyncContext asyncContext) {
      if (!listenerAttached.compareAndSet(false, true)) {
        return;
      }

      asyncContext.addListener(
          new AsyncRequestCompletionListener(request, response, context), request, response);
    }

    boolean isNotAsync() {
      return !listenerAttached.get();
    }
  }

  private class AsyncRequestCompletionListener implements AsyncListener {
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final Context context;
    private final AtomicBoolean responseHandled = new AtomicBoolean();

    AsyncRequestCompletionListener(
        HttpServletRequest request, HttpServletResponse response, Context context) {
      this.request = request;
      this.response = response;
      this.context = context;
    }

    @Override
    public void onComplete(AsyncEvent asyncEvent) {
      if (responseHandled.compareAndSet(false, true)) {
        instrumenter.end(context, request, response, null);
      }
    }

    @Override
    public void onTimeout(AsyncEvent asyncEvent) {
      if (responseHandled.compareAndSet(false, true)) {
        instrumenter.end(context, request, response, null);
      }
    }

    @Override
    public void onError(AsyncEvent asyncEvent) {
      if (responseHandled.compareAndSet(false, true)) {
        instrumenter.end(context, request, response, asyncEvent.getThrowable());
      }
    }

    @Override
    public void onStartAsync(AsyncEvent asyncEvent) {
      asyncEvent
          .getAsyncContext()
          .addListener(this, asyncEvent.getSuppliedRequest(), asyncEvent.getSuppliedResponse());
    }
  }
}
