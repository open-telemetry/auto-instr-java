/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.play.v2_4;

import static io.opentelemetry.javaagent.instrumentation.play.v2_4.Play24Singletons.instrumenter;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

/** Container used to carry state between enter and exit advices */
public final class ActionScope {

  private final ActionData actionData;
  private final Context context;
  private final Scope scope;

  public ActionScope(Context context, Scope scope, ActionData actionData) {
    this.actionData = actionData;
    this.context = context;
    this.scope = scope;
  }

  public Context getContext() {
    return context;
  }

  public Scope getScope() {
    return scope;
  }

  public static ActionScope start(Context parentContext, ActionData actionData) {
    if (!instrumenter().shouldStart(parentContext, actionData)) {
      return null;
    }

    Context context = instrumenter().start(parentContext, actionData);
    return new ActionScope(context, context.makeCurrent(), actionData);
  }

  public void closeScope() {
    if (scope != null) {
      scope.close();
    }
  }

  public void end(Throwable throwable) {
    closeScope();
    instrumenter().end(context, actionData, null, throwable);
  }
}
