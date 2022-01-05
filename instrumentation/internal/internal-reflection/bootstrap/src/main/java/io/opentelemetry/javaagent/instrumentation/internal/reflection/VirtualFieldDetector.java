/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.internal.reflection;

import io.opentelemetry.instrumentation.api.cache.Cache;

/** Helper class for detecting whether given class has virtual fields. */
public final class VirtualFieldDetector {

  private static final Cache<Class<?>, Boolean> classesWithVirtualFields = Cache.weak();

  private VirtualFieldDetector() {}

  /**
   * Detect whether given class has virtual fields. This method looks for virtual fields only from
   * the specified class not its super classes.
   *
   * @param clazz a class
   * @return true if given class has virtual fields
   */
  public static boolean hasVirtualFields(Class<?> clazz) {
    // clazz.getInterfaces() needs to be called before reading from classesWithVirtualFields
    // as the call to clazz.getInterfaces() triggers adding clazz to that map via instrumentation
    // calling VirtualFieldDetector#markVirtualFieldsPresent() from Class#getInterfaces()
    clazz.getInterfaces();
    return classesWithVirtualFields.get(clazz) != null;
  }

  static void markVirtualFieldsPresent(Class<?> clazz) {
    classesWithVirtualFields.put(clazz, Boolean.TRUE);
  }
}
