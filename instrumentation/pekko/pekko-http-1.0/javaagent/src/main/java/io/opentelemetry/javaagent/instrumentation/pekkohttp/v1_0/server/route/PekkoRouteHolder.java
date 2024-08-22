/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.pekkohttp.v1_0.server.route;

import static io.opentelemetry.context.ContextKey.named;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.context.ImplicitContextKeyed;
import io.opentelemetry.instrumentation.api.semconv.http.HttpServerRoute;
import io.opentelemetry.instrumentation.api.semconv.http.HttpServerRouteSource;
import java.util.ArrayDeque;
import java.util.Deque;

public class PekkoRouteHolder implements ImplicitContextKeyed {
  private static final ContextKey<PekkoRouteHolder> KEY = named("opentelemetry-pekko-route");

  private String route = "";
  private boolean newSegment = true;
  private boolean endMatched;
  private final Deque<String> stack = new ArrayDeque<>();

  public static Context init(Context context) {
    if (context.get(KEY) != null) {
      return context;
    }
    return context.with(new PekkoRouteHolder());
  }

  public static void push(String path) {
    PekkoRouteHolder holder = Context.current().get(KEY);
    if (holder != null && holder.newSegment && !holder.endMatched) {
      holder.route += path;
      holder.newSegment = false;
    }
  }

  public static void startSegment() {
    PekkoRouteHolder holder = Context.current().get(KEY);
    if (holder != null) {
      holder.newSegment = true;
    }
  }

  public static void endMatched() {
    Context context = Context.current();
    PekkoRouteHolder holder = context.get(KEY);
    if (holder != null) {
      holder.endMatched = true;
      HttpServerRoute.update(context, HttpServerRouteSource.CONTROLLER, holder.route);
    }
  }

  public static void save() {
    PekkoRouteHolder holder = Context.current().get(KEY);
    if (holder != null) {
      holder.stack.push(holder.route);
      holder.newSegment = true;
    }
  }

  public static void reset() {
    PekkoRouteHolder holder = Context.current().get(KEY);
    if (holder != null) {
      holder.route = holder.stack.peek();
      holder.newSegment = true;
    }
  }

  public static void restore() {
    PekkoRouteHolder holder = Context.current().get(KEY);
    if (holder != null) {
      holder.route = holder.stack.pop();
      holder.newSegment = true;
    }
  }

  @Override
  public Context storeInContext(Context context) {
    return context.with(KEY, this);
  }

  private PekkoRouteHolder() {}
}
