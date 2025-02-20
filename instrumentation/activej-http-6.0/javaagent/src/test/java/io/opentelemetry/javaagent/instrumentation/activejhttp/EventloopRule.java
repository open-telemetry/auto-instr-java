/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.activejhttp;

import static io.activej.common.exception.FatalErrorHandlers.rethrow;

import io.activej.eventloop.Eventloop;
import io.activej.reactor.Reactor;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public final class EventloopRule implements TestRule {

  static {
    createEventloop();
  }

  private static void createEventloop() {
    Eventloop.builder().withCurrentThread().withFatalErrorHandler(rethrow()).build();
  }

  @Override
  public Statement apply(Statement base, Description description) {
    Reactor currentReactor = Reactor.getCurrentReactor();
    if (!(currentReactor instanceof Eventloop) || !currentReactor.inReactorThread()) {
      createEventloop();
    }
    return base;
  }
}
