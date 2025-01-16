/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.aerospike.internal;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class CustomElementMatcher {
  private CustomElementMatcher() {}

  public static ElementMatcher<Iterable<? extends TypeDescription.Generic>> iterableHasAtLeastOne(
      ElementMatcher<TypeDescription.Generic> matcher) {
    return iterable -> {
      for (TypeDescription.Generic typeDescription : iterable) {
        if (matcher.matches(typeDescription)) {
          return true;
        }
      }
      return false;
    };
  }
}
