plugins {
  id("otel.java-conventions")
}

dependencies {
  api(project(":testing-common"))

  implementation("org.apache.kafka:kafka-clients:0.11.0.0")

  implementation(project(":instrumentation:kafka:kafka-clients:kafka-clients-common-0.11:library"))

  implementation("org.testcontainers:kafka")
  implementation("org.testcontainers:junit-jupiter")

  compileOnly("com.google.auto.value:auto-value-annotations")
  annotationProcessor("com.google.auto.value:auto-value")
}
