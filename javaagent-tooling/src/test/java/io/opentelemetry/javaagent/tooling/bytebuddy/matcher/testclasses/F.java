/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.tooling.bytebuddy.matcher.testclasses;

@SuppressWarnings("ClassNamedLikeTypeParameter")
public abstract class F implements E {
  @Trace
  public abstract void f();
}
