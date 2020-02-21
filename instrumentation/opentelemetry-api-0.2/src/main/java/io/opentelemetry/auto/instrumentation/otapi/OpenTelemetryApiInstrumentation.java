package io.opentelemetry.auto.instrumentation.otapi;

import static io.opentelemetry.auto.tooling.ByteBuddyElementMatchers.safeHasSuperType;
import static net.bytebuddy.matcher.ElementMatchers.isMethod;
import static net.bytebuddy.matcher.ElementMatchers.isPublic;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import com.google.auto.service.AutoService;
import io.opentelemetry.auto.tooling.Instrumenter;
import java.util.HashMap;
import java.util.Map;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

@AutoService(Instrumenter.class)
public class OpenTelemetryApiInstrumentation extends Instrumenter.Default {
  public OpenTelemetryApiInstrumentation() {
    super("opentelemetry-api");
  }

  @Override
  public ElementMatcher<? super TypeDescription> typeMatcher() {
    return safeHasSuperType(named("unshaded.io.opentelemetry.OpenTelemetry"));
  }

  @Override
  public String[] helperClassNames() {
    return new String[] {
      packageName + ".Bridging",
      packageName + ".Bridging$1",
      packageName + ".UnshadedBinaryFormat",
      packageName + ".UnshadedHttpTextFormat",
      packageName + ".UnshadedHttpTextFormat$UnshadedSetter",
      packageName + ".UnshadedHttpTextFormat$UnshadedGetter",
      packageName + ".UnshadedScope",
      packageName + ".UnshadedSpan",
      packageName + ".UnshadedSpanBuilder",
      packageName + ".UnshadedTracer",
      packageName + ".UnshadedTracerFactory"
    };
  }

  @Override
  public Map<? extends ElementMatcher<? super MethodDescription>, String> transformers() {
    final Map<ElementMatcher<? super MethodDescription>, String> transformers = new HashMap<>();
    transformers.put(
        isMethod().and(isPublic()).and(named("getTracerFactory")).and(takesArguments(0)),
        OpenTelemetryApiInstrumentation.class.getName() + "$GetTracerFactoryAdvice");
    return transformers;
  }

  public static class GetTracerFactoryAdvice {

    @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
    public static void methodExit(
        @Advice.Return(readOnly = false)
            unshaded.io.opentelemetry.trace.TracerFactory tracerFactory) {
      tracerFactory = new UnshadedTracerFactory();
    }
  }
}
