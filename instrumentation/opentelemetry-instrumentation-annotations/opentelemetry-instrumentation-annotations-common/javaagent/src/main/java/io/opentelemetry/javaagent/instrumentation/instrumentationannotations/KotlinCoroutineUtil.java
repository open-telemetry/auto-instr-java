/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.instrumentationannotations;

import static net.bytebuddy.matcher.ElementMatchers.returns;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterList;
import net.bytebuddy.matcher.ElementMatcher;

public final class KotlinCoroutineUtil {

  private KotlinCoroutineUtil() {}

  public static ElementMatcher<MethodDescription> isKotlinSuspendMethod() {
    // kotlin suspend methods return Object and take kotlin.coroutines.Continuation as last argument
    return returns(Object.class)
        .and(
            target -> {
              ParameterList<?> parameterList = target.getParameters();
              if (!parameterList.isEmpty()) {
                String lastParameter =
                    parameterList.get(parameterList.size() - 1).getType().asErasure().getName();
                return "kotlin.coroutines.Continuation".equals(lastParameter);
              }
              return false;
            });
  }
}
