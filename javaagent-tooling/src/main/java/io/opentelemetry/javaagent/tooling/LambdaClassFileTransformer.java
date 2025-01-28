/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.tooling;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

/** {@link ClassFileTransformer} for lambda instrumentation without jpms modules */
public class LambdaClassFileTransformer implements ClassFileTransformer {

  private final ClassFileTransformer delegate;

  public LambdaClassFileTransformer(ClassFileTransformer delegate) {
    this.delegate = delegate;
  }

  @Override
  public byte[] transform(
      ClassLoader loader,
      String className,
      Class<?> classBeingRedefined,
      ProtectionDomain protectionDomain,
      byte[] classfileBuffer)
      throws IllegalClassFormatException {

    // lambda instrumentation happens only when the lambda is defined, thus the classBeingRedefined
    // must be null otherwise we get a partial instrumentation, for example virtual fields are not
    // properly applied.
    return delegate.transform(loader, className, null, null, classfileBuffer);
  }
}
