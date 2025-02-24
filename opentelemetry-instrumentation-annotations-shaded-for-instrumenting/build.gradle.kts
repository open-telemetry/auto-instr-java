plugins {
  id("com.gradleup.shadow")
  id("otel.java-conventions")
}

description = "opentelemetry-instrumentation-annotations shaded for internal javaagent usage"
group = "io.opentelemetry.javaagent"

dependencies {
  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations")
  implementation("io.opentelemetry.instrumentation:opentelemetry-instrumentation-annotations-incubator")
}

// OpenTelemetry Instrumentation Annotations shaded so that it can be used in instrumentation of
// OpenTelemetry Instrumentation Annotations itself,
// and then its usage can be unshaded after OpenTelemetry Instrumentation Annotations is shaded
// (see more explanation in opentelemetry-api-1.0.gradle)
tasks {
  shadowJar {
    relocate("io.opentelemetry.instrumentation.annotations", "application.io.opentelemetry.instrumentation.annotations")
    relocate("io.opentelemetry.api.trace.SpanKind", "application.io.opentelemetry.api.trace.SpanKind")
  }
}
