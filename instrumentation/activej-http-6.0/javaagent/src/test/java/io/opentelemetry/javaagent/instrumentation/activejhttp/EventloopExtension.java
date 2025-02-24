/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.activejhttp;

import static io.activej.common.exception.FatalErrorHandlers.rethrow;

import io.activej.eventloop.Eventloop;
import io.activej.reactor.Reactor;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public class EventloopExtension implements BeforeEachCallback, AfterAllCallback {

  static {
    createEventloop();
  }

  private static void createEventloop() {
    Eventloop.builder().withCurrentThread().withFatalErrorHandler(rethrow()).build();
  }

  @Override
  public void beforeEach(ExtensionContext context) {
    Reactor currentReactor = Reactor.getCurrentReactor();
    if (!(currentReactor instanceof Eventloop) || !currentReactor.inReactorThread()) {
      createEventloop();
    }
  }

  @Override
  public void afterAll(ExtensionContext context) {
    //
  }
}
