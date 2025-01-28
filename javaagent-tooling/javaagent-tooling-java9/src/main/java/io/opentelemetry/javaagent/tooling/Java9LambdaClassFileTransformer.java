/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.tooling;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

/** {@link ClassFileTransformer} for lambda instrumentation with java9 jpms module compatibility */
public class Java9LambdaClassFileTransformer implements ClassFileTransformer {

  private final ClassFileTransformer delegate;

  public Java9LambdaClassFileTransformer(ClassFileTransformer delegate) {
    this.delegate = delegate;
  }

  @Override
  public byte[] transform(
      ClassLoader loader,
      String className,
      Class<?> targetClass,
      ProtectionDomain protectionDomain,
      byte[] classfileBuffer)
      throws IllegalClassFormatException {

    if (targetClass == null) {
      // we expect the target class argument to be non-null, however it's only used to resolve
      // the JPMS module
      throw new IllegalStateException("can't resolve module without class");
    }
    Module module = targetClass.getModule();

    // lambda instrumentation happens only when the lambda is defined, thus the classBeingRedefined
    // must be null otherwise we get a partial instrumentation, for example virtual fields are not
    // properly applied
    return delegate.transform(module, loader, className, null, null, classfileBuffer);
  }
}
