/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package muzzle;

import io.opentelemetry.instrumentation.TestHelperClasses.Helper;
import net.bytebuddy.asm.Advice;

public class TestClasses {

  public static class MethodBodyAdvice {
    @Advice.OnMethodEnter
    public static void methodBodyAdvice() {
      A a = new A();
      SomeInterface inter = new SomeImplementation();
      inter.someMethod();
      a.publicB.method("foo");
      a.publicB.methodWithPrimitives(false);
      a.publicB.methodWithArrays(new String[0]);
      B.staticMethod();
      A.staticB.method("bar");
    }

    public static class A {
      public B publicB = new B();
      protected Object protectedField = null;
      private final Object privateField = null;
      public static B staticB = new B();
    }

    public static class B {
      public String method(String s) {
        return s;
      }

      public void methodWithPrimitives(boolean b) {}

      public Object[] methodWithArrays(String[] s) {
        return s;
      }

      private void privateStuff() {}

      protected void protectedMethod() {}

      public static void staticMethod() {}
    }

    public static class B2 extends B {
      public void stuff() {
        B b = new B();
        b.protectedMethod();
      }
    }

    public static class A2 extends A {}

    public interface SomeInterface {
      void someMethod();
    }

    public static class SomeImplementation implements SomeInterface {
      @Override
      public void someMethod() {}
    }

    public static class SomeClassWithFields {
      public int instanceField = 0;
      public static int staticField = 0;
      public final int finalField = 0;
    }

    public interface AnotherInterface extends SomeInterface {}
  }

  public static class LdcAdvice {
    public static void ldcMethod() {
      MethodBodyAdvice.A.class.getName();
    }
  }

  public static class InstanceofAdvice {
    public static boolean instanceofMethod(Object a) {
      return a instanceof MethodBodyAdvice.A;
    }
  }

  // TODO Can't test this until java 7 is dropped.
  public static class InDyAdvice {
    //    public static MethodBodyAdvice.SomeInterface indyMethod(
    //        final MethodBodyAdvice.SomeImplementation a) {
    //      Runnable aStaticMethod = MethodBodyAdvice.B::aStaticMethod;
    //      return a::someMethod;
    //    }
  }

  public static class HelperAdvice {
    public static void adviceMethod() {
      Helper h = new Helper();
    }
  }
}
