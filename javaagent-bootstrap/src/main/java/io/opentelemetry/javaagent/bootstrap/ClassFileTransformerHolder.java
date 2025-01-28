/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.bootstrap;

import java.lang.instrument.ClassFileTransformer;

/**
 * Holder for {@link ClassFileTransformer} used by the instrumentation. Calling transform on this
 * class file transformer processes given bytes the same way as they would be processed during
 * loading of the class.
 */
public final class ClassFileTransformerHolder {

  private static volatile ClassFileTransformer classFileTransformer;

  /**
   * get class transformer for defining lambdas
   *
   * @return class transformer for defining lambdas
   */
  public static ClassFileTransformer getLambdaClassFileTransformer() {
    return classFileTransformer;
  }

  /**
   * set class transformer for defining lambdas
   *
   * @param transformer transformer
   */
  public static void setLambdaClassFileTransformer(ClassFileTransformer transformer) {
    classFileTransformer = transformer;
  }

  private ClassFileTransformerHolder() {}
}
