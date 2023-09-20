/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.reactornetty.v1_0;

import static io.opentelemetry.javaagent.instrumentation.reactornetty.v1_0.ReactorNettySingletons.instrumenter;

import io.opentelemetry.context.Context;
import io.opentelemetry.instrumentation.api.instrumenter.http.HttpClientResendCount;
import io.opentelemetry.instrumentation.api.internal.InstrumenterUtil;
import io.opentelemetry.instrumentation.api.internal.Timer;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import javax.annotation.Nullable;
import reactor.netty.http.client.HttpClientRequest;
import reactor.netty.http.client.HttpClientResponse;

final class InstrumentationContexts {

  private static final AtomicReferenceFieldUpdater<InstrumentationContexts, Context>
      parentContextUpdater =
          AtomicReferenceFieldUpdater.newUpdater(
              InstrumentationContexts.class, Context.class, "parentContext");

  private volatile Context parentContext;
  private volatile Timer timer;
  // on retries, reactor-netty starts the next resend attempt before it ends the previous one (i.e.
  // it calls the callback functions in that order); thus for a short moment there can be multiple
  // coexisting HTTP client spans
  private final Queue<RequestAndContext> clientContexts = new LinkedBlockingQueue<>();

  void initialize(Context parentContext) {
    Context parentContextWithResends = HttpClientResendCount.initialize(parentContext);
    // make sure initialization happens only once
    if (parentContextUpdater.compareAndSet(this, null, parentContextWithResends)) {
      timer = Timer.start();
    }
  }

  Context getParentContext() {
    return parentContext;
  }

  @Nullable
  Context getClientContext() {
    RequestAndContext requestAndContext = clientContexts.peek();
    return requestAndContext == null ? null : requestAndContext.context;
  }

  @Nullable
  Context startClientSpan(HttpClientRequest request) {
    Context parentContext = this.parentContext;
    Context context = null;
    if (instrumenter().shouldStart(parentContext, request)) {
      context = instrumenter().start(parentContext, request);
      clientContexts.offer(new RequestAndContext(request, context));
    }
    return context;
  }

  // we are synchronizing here to ensure that spans are ended in the oder they are read from the
  // queue
  synchronized void endClientSpan(
      @Nullable HttpClientResponse response, @Nullable Throwable error) {
    RequestAndContext requestAndContext = clientContexts.poll();
    if (requestAndContext != null) {
      instrumenter().end(requestAndContext.context, requestAndContext.request, response, error);
    }
  }

  void startAndEndConnectionErrorSpan(HttpClientRequest request, Throwable error) {
    Context parentContext = this.parentContext;
    if (instrumenter().shouldStart(parentContext, request)) {
      Timer timer = this.timer;
      InstrumenterUtil.startAndEnd(
          instrumenter(), parentContext, request, null, error, timer.startTime(), timer.now());
    }
  }

  static final class RequestAndContext {
    final HttpClientRequest request;
    final Context context;

    RequestAndContext(HttpClientRequest request, Context context) {
      this.request = request;
      this.context = context;
    }
  }
}
