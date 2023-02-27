plugins {
  id("otel.library-instrumentation")
}

dependencies {
  library("com.datastax.oss:java-driver-core:4.4.0")

  compileOnly("com.google.auto.value:auto-value-annotations")
  annotationProcessor("com.google.auto.value:auto-value")
}
