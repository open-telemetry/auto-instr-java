/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.internal.classloader;

import static java.util.logging.Level.WARNING;

import io.opentelemetry.javaagent.bootstrap.BootstrapPackagePrefixesHolder;
import io.opentelemetry.javaagent.tooling.Constants;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.List;
import java.util.logging.Logger;

public class BootstrapPackagesHelper {

  public static final List<String> bootstrapPackagesPrefixes = findBootstrapPackagePrefixes();

  /**
   * We have to make sure that {@link BootstrapPackagePrefixesHolder} is loaded from bootstrap class
   * loader. After that we can use in {@link BootDelegationInstrumentation.LoadClassAdvice}.
   */
  private static List<String> findBootstrapPackagePrefixes() {
    try {
      Class<?> holderClass =
          Class.forName(
              "io.opentelemetry.javaagent.bootstrap.BootstrapPackagePrefixesHolder", true, null);
      MethodHandle methodHandle =
          MethodHandles.publicLookup()
              .findStatic(
                  holderClass, "getBoostrapPackagePrefixes", MethodType.methodType(List.class));
      //noinspection unchecked
      return (List<String>) methodHandle.invokeExact();
    } catch (Throwable e) {
      Logger.getLogger(BootstrapPackagesHelper.class.getName())
          .log(WARNING, "Unable to load bootstrap package prefixes from the bootstrap CL", e);
      return Constants.BOOTSTRAP_PACKAGE_PREFIXES;
    }
  }

  private BootstrapPackagesHelper() {}
}
