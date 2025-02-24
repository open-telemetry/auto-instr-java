plugins {
  id("otel.library-instrumentation")
}

dependencies {
  compileOnly("com.google.auto.value:auto-value-annotations")
  annotationProcessor("com.google.auto.value:auto-value")

  implementation(project(":instrumentation:netty:netty-common:library"))

  compileOnly("io.netty:netty-codec-http:4.0.0.Final")
}
