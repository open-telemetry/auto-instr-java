/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.tooling;

import io.opentelemetry.javaagent.bootstrap.AgentInitializer;
import io.opentelemetry.javaagent.bootstrap.AgentStarter;
import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.security.ProtectionDomain;
import net.bytebuddy.jar.asm.ClassReader;
import net.bytebuddy.jar.asm.ClassVisitor;
import net.bytebuddy.jar.asm.ClassWriter;
import net.bytebuddy.jar.asm.MethodVisitor;
import net.bytebuddy.jar.asm.Opcodes;
import net.bytebuddy.jar.asm.Type;

/**
 * Main entry point into code that is running inside agent class loader, used reflectively from
 * {@code io.opentelemetry.javaagent.bootstrap.AgentInitializer}.
 */
public class AgentStarterImpl implements AgentStarter {
  private final Instrumentation instrumentation;
  private final File javaagentFile;
  private ClassLoader extensionClassLoader;

  public AgentStarterImpl(Instrumentation instrumentation, File javaagentFile) {
    this.instrumentation = instrumentation;
    this.javaagentFile = javaagentFile;
  }

  @Override
  public boolean delayStart() {
    LaunchHelperClassFileTransformer transformer = new LaunchHelperClassFileTransformer();
    instrumentation.addTransformer(transformer, true);

    try {
      Class<?> c = Class.forName("sun.launcher.LauncherHelper", false, null);
      instrumentation.retransformClasses(c);
      return transformer.hookInserted;
    } catch (ClassNotFoundException | UnmodifiableClassException ignore) {
      // ignore
    } finally {
      instrumentation.removeTransformer(transformer);
    }

    return false;
  }

  @Override
  public void start() {
    extensionClassLoader = createExtensionClassLoader(getClass().getClassLoader(), javaagentFile);
    ClassLoader savedContextClassLoader = Thread.currentThread().getContextClassLoader();
    try {
      Thread.currentThread().setContextClassLoader(extensionClassLoader);
      AgentInstaller.installBytebuddyAgent(instrumentation);
    } finally {
      Thread.currentThread().setContextClassLoader(savedContextClassLoader);
    }
  }

  @Override
  public ClassLoader getExtensionClassLoader() {
    return extensionClassLoader;
  }

  private static ClassLoader createExtensionClassLoader(
      ClassLoader agentClassLoader, File javaagentFile) {
    return ExtensionClassLoader.getInstance(agentClassLoader, javaagentFile);
  }

  private static class LaunchHelperClassFileTransformer implements ClassFileTransformer {
    boolean hookInserted = false;

    @Override
    public byte[] transform(
        ClassLoader loader,
        String className,
        Class<?> classBeingRedefined,
        ProtectionDomain protectionDomain,
        byte[] classfileBuffer) {
      if (!"sun/launcher/LauncherHelper".equals(className)) {
        return null;
      }
      ClassReader cr = new ClassReader(classfileBuffer);
      ClassWriter cw = new ClassWriter(cr, 0);
      ClassVisitor cv =
          new ClassVisitor(Opcodes.ASM7, cw) {
            @Override
            public MethodVisitor visitMethod(
                int access, String name, String descriptor, String signature, String[] exceptions) {
              MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);
              if ("checkAndLoadMain".equals(name)) {
                return new MethodVisitor(api, mv) {
                  @Override
                  public void visitCode() {
                    super.visitCode();
                    hookInserted = true;
                    mv.visitMethodInsn(
                        Opcodes.INVOKESTATIC,
                        Type.getInternalName(AgentInitializer.class),
                        "delayedStartHook",
                        "()V",
                        false);
                  }
                };
              }
              return mv;
            }
          };
      cr.accept(cv, 0);

      return hookInserted ? cw.toByteArray() : null;
    }
  }
}
