plugins {
  id("otel.library-instrumentation")
}

dependencies {
  compileOnly("io.opentelemetry:opentelemetry-sdk")
}
