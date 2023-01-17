plugins {
  id("otel.java-conventions")
  id("otel.japicmp-conventions")
  id("otel.publish-conventions")
}

group = "io.opentelemetry.javaagent"

dependencies {
  api("io.opentelemetry:opentelemetry-sdk")
  api("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure-spi")
  api("net.bytebuddy:byte-buddy-dep")

  implementation(project(":instrumentation-api"))

  // autoconfigure is unstable, do not expose as api
  implementation("io.opentelemetry:opentelemetry-sdk-extension-autoconfigure")

  // Used by byte-buddy but not brought in as a transitive dependency.
  compileOnly("com.google.code.findbugs:annotations")
}