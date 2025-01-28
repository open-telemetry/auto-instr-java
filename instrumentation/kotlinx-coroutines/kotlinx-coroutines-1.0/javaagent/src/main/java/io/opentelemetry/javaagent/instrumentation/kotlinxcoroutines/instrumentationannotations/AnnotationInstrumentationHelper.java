/*
 * Copyright The OpenTelemetry Authors
 * SPDX-License-Identifier: Apache-2.0
 */

package io.opentelemetry.javaagent.instrumentation.kotlinxcoroutines.instrumentationannotations;

import static io.opentelemetry.javaagent.instrumentation.kotlinxcoroutines.instrumentationannotations.AnnotationSingletons.instrumenter;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.util.VirtualField;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.intrinsics.IntrinsicsKt;

/**
 * Instrumentation helper that is called through bytecode instrumentation. When using invokedynamic
 * instrumentation this class is called through an injected proxy, and thus it should not pull any
 * other class references than the ones that are already present in the target classloader or the
 * bootstrap classloader. This is why the {@link MethodRequest} class is here passed as an {@link
 * Object} as it allows to avoid having to inject extra classes in the target classloader
 */
@SuppressWarnings("unused") // methods calls injected through bytecode instrumentation
public final class AnnotationInstrumentationHelper {

  private static final VirtualField<Continuation<?>, Context> contextField =
      VirtualField.find(Continuation.class, Context.class);

  public static Object createMethodRequest(
      Class<?> declaringClass, String methodName, String withSpanValue, String spanKindString) {
    SpanKind spanKind = SpanKind.INTERNAL;
    if (spanKindString != null) {
      try {
        spanKind = SpanKind.valueOf(spanKindString);
      } catch (IllegalArgumentException exception) {
        // ignore
      }
    }

    return MethodRequest.create(declaringClass, methodName, withSpanValue, spanKind);
  }

  public static Context enterCoroutine(int label, Continuation<?> continuation, Object request) {
    // label 0 means that coroutine is started, any other label means that coroutine is resumed
    if (label == 0) {
      Context context = instrumenter().start(Context.current(), (MethodRequest) request);
      // null continuation means that this method is not going to be resumed, and we don't need to
      // store the context
      if (continuation != null) {
        contextField.set(continuation, context);
      }
      return context;
    } else {
      return continuation != null ? contextField.get(continuation) : null;
    }
  }

  public static Scope openScope(Context context) {
    return context != null ? context.makeCurrent() : null;
  }

  public static void exitCoroutine(
      Object result, Object request, Continuation<?> continuation, Context context, Scope scope) {
    exitCoroutine(null, result, request, continuation, context, scope);
  }

  public static void exitCoroutine(
      Throwable error,
      Object result,
      Object request,
      Continuation<?> continuation,
      Context context,
      Scope scope) {
    if (scope == null) {
      return;
    }
    scope.close();

    // end the span when this method can not be resumed (coroutine is null) or if it has reached
    // final state (returns anything else besides COROUTINE_SUSPENDED)
    if (continuation == null || result != IntrinsicsKt.getCOROUTINE_SUSPENDED()) {
      instrumenter().end(context, (MethodRequest) request, null, error);
    }
  }

  public static void setSpanAttribute(int label, String name, boolean value) {
    // only add the attribute when coroutine is started
    if (label == 0) {
      Span.current().setAttribute(name, value);
    }
  }

  public static void setSpanAttribute(int label, String name, byte value) {
    // only add the attribute when coroutine is started
    if (label == 0) {
      Span.current().setAttribute(name, value);
    }
  }

  public static void setSpanAttribute(int label, String name, char value) {
    // only add the attribute when coroutine is started
    if (label == 0) {
      Span.current().setAttribute(name, String.valueOf(value));
    }
  }

  public static void setSpanAttribute(int label, String name, double value) {
    // only add the attribute when coroutine is started
    if (label == 0) {
      Span.current().setAttribute(name, value);
    }
  }

  public static void setSpanAttribute(int label, String name, float value) {
    // only add the attribute when coroutine is started
    if (label == 0) {
      Span.current().setAttribute(name, value);
    }
  }

  public static void setSpanAttribute(int label, String name, int value) {
    // only add the attribute when coroutine is started
    if (label == 0) {
      Span.current().setAttribute(name, value);
    }
  }

  public static void setSpanAttribute(int label, String name, long value) {
    // only add the attribute when coroutine is started
    if (label == 0) {
      Span.current().setAttribute(name, value);
    }
  }

  public static void setSpanAttribute(int label, String name, short value) {
    // only add the attribute when coroutine is started
    if (label == 0) {
      Span.current().setAttribute(name, value);
    }
  }

  public static void setSpanAttribute(int label, String name, Object value) {
    // only add the attribute when coroutine is started
    if (label != 0) {
      return;
    }
    if (value instanceof String) {
      Span.current().setAttribute(name, (String) value);
    } else if (value instanceof Boolean) {
      Span.current().setAttribute(name, (Boolean) value);
    } else if (value instanceof Byte) {
      Span.current().setAttribute(name, (Byte) value);
    } else if (value instanceof Character) {
      Span.current().setAttribute(name, (Character) value);
    } else if (value instanceof Double) {
      Span.current().setAttribute(name, (Double) value);
    } else if (value instanceof Float) {
      Span.current().setAttribute(name, (Float) value);
    } else if (value instanceof Integer) {
      Span.current().setAttribute(name, (Integer) value);
    } else if (value instanceof Long) {
      Span.current().setAttribute(name, (Long) value);
    }
    // TODO: arrays and List not supported see AttributeBindingFactoryTest
  }

  public static void init() {}

  private AnnotationInstrumentationHelper() {}
}
