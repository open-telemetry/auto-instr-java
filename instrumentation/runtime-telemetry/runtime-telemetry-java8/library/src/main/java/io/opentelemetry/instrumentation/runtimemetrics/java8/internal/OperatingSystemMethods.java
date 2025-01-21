/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.instrumentation.runtimemetrics.java8.internal;

import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.function.Supplier;
import javax.annotation.Nullable;

/**
 * This class is internal and is hence not for public use. Its APIs are unstable and can change at
 * any time.
 */
public final class OperatingSystemMethods {

  private static final String OS_BEAN_J9 = "com.ibm.lang.management.OperatingSystemMXBean";
  private static final String OS_BEAN_HOTSPOT = "com.sun.management.OperatingSystemMXBean";
  private static final String UNIX_OS_BEAN_J9 = "com.ibm.lang.management.UnixOperatingSystemMXBean";
  private static final String UNIX_OS_BEAN_HOTSPOT = "com.sun.management.UnixOperatingSystemMXBean";
  private static final String METHOD_PROCESS_CPU_TIME = "getProcessCpuTime";
  private static final String METHOD_PROCESS_CPU_LOAD = "getProcessCpuLoad";
  private static final String METHOD_CPU_LOAD = "getCpuLoad";
  private static final String METHOD_SYSTEM_CPU_LOAD = "getSystemCpuLoad";
  private static final String METHOD_OPEN_FILE_DESCRIPTOR_COUNT = "getOpenFileDescriptorCount";
  private static final String METHOD_MAX_FILE_DESCRIPTOR_COUNT = "getMaxFileDescriptorCount";

  @Nullable private static final Supplier<Long> processCpuTime;
  @Nullable private static final Supplier<Double> processCpuUtilization;
  @Nullable private static final Supplier<Double> systemCpuUtilization;
  @Nullable private static final Supplier<Long> openFileDescriptorCount;
  @Nullable private static final Supplier<Long> maxFileDescriptorCount;

  static {
    OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();

    processCpuTime =
        findMethod(osBean, OS_BEAN_HOTSPOT, OS_BEAN_J9, METHOD_PROCESS_CPU_TIME, Long.class);

    processCpuUtilization =
        findMethod(osBean, OS_BEAN_HOTSPOT, OS_BEAN_J9, METHOD_PROCESS_CPU_LOAD, Double.class);

    // As of java 14, com.sun.management.OperatingSystemMXBean#getCpuLoad() is preferred and
    // #getSystemCpuLoad() is deprecated
    Supplier<Double> systemCpuSupplier =
        methodInvoker(osBean, OS_BEAN_HOTSPOT, METHOD_CPU_LOAD, Double.class);
    if (systemCpuSupplier == null) {
      systemCpuSupplier =
          findMethod(osBean, OS_BEAN_HOTSPOT, OS_BEAN_J9, METHOD_SYSTEM_CPU_LOAD, Double.class);
    }
    systemCpuUtilization = systemCpuSupplier;

    openFileDescriptorCount =
        findMethod(
            osBean,
            UNIX_OS_BEAN_HOTSPOT,
            UNIX_OS_BEAN_J9,
            METHOD_OPEN_FILE_DESCRIPTOR_COUNT,
            Long.class);

    maxFileDescriptorCount =
        findMethod(
            osBean,
            UNIX_OS_BEAN_HOTSPOT,
            UNIX_OS_BEAN_J9,
            METHOD_MAX_FILE_DESCRIPTOR_COUNT,
            Long.class);
  }

  @Nullable
  @SuppressWarnings("ReturnValueIgnored")
  private static <T extends Number> Supplier<T> methodInvoker(
      OperatingSystemMXBean osBean,
      String osBeanClassName,
      String methodName,
      Class<T> returnType) {
    try {
      Class<?> osBeanClass = Class.forName(osBeanClassName);
      osBeanClass.cast(osBean);
      Method method = osBeanClass.getDeclaredMethod(methodName);
      return () -> {
        try {
          return returnType.cast(method.invoke(osBean));
        } catch (IllegalAccessException | InvocationTargetException e) {
          return null;
        }
      };
    } catch (ClassNotFoundException | ClassCastException | NoSuchMethodException e) {
      return null;
    }
  }

  // judge whether use hotspots or openj9
  private static <T extends Number> Supplier<T> findMethod(
      OperatingSystemMXBean osBean,
      String osBeanClassName,
      String osBeanJ9ClassName,
      String methodName,
      Class<T> returnType) {
    Supplier<T> processSupplier = methodInvoker(osBean, osBeanClassName, methodName, returnType);
    if (processSupplier == null) {
      // More users will be on hotspot than j9, so check for j9 second
      processSupplier = methodInvoker(osBean, osBeanJ9ClassName, methodName, returnType);
    }

    return processSupplier;
  }

  public static Supplier<Long> processCpuTime() {
    return processCpuTime;
  }

  public static Supplier<Double> processCpuUtilization() {
    return processCpuUtilization;
  }

  public static Supplier<Double> systemCpuUtilization() {
    return systemCpuUtilization;
  }

  public static Supplier<Long> openFileDescriptorCount() {
    return openFileDescriptorCount;
  }

  public static Supplier<Long> maxFileDescriptorCount() {
    return maxFileDescriptorCount;
  }

  private OperatingSystemMethods() {}
}
