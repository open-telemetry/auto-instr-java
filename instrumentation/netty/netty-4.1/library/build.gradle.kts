plugins {
  id("otel.library-instrumentation")
}

dependencies {
  library("io.netty:netty-codec-http:4.1.0.Final")
  implementation(project(":instrumentation:netty:netty-common-4.0:library"))
  implementation(project(":instrumentation:netty:netty-common:library"))

  compileOnly("com.google.auto.value:auto-value-annotations")
  annotationProcessor("com.google.auto.value:auto-value")

  testImplementation(project(":instrumentation:netty:netty-4.1:testing"))
}
