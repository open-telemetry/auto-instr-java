/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.pekkohttp.v1_0.server.route;

import org.apache.pekko.http.scaladsl.server.RouteResult;
import scala.PartialFunction;
import scala.Unit;
import scala.util.Try;

public class RestoreOnExit implements PartialFunction<Try<RouteResult>, Unit> {
  @Override
  public boolean isDefinedAt(Try<RouteResult> x) {
    return true;
  }

  @Override
  public Unit apply(Try<RouteResult> v1) {
    PekkoRouteHolder.restore();
    return null;
  }
}
