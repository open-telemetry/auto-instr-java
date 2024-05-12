plugins {
  id("otel.java-conventions")
  id("otel.jacoco-conventions")
  id("otel.japicmp-conventions")
  id("otel.publish-conventions")

  jacoco
}

group = "io.opentelemetry.instrumentation"

dependencies {
  implementation(project(":instrumentation-api"))

  api("io.opentelemetry:opentelemetry-api")
  api("io.opentelemetry.semconv:opentelemetry-semconv")
  api("io.opentelemetry.semconv:opentelemetry-semconv-incubating")

  compileOnly("com.google.auto.value:auto-value-annotations")
  annotationProcessor("com.google.auto.value:auto-value")

  testImplementation(project(":testing-common"))
  testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
}

tasks.test {
  finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
  dependsOn(tasks.test)
}
