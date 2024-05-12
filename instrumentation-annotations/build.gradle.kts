plugins {
  id("otel.java-conventions")
  id("otel.japicmp-conventions")
  id("otel.publish-conventions")

  id("otel.animalsniffer-conventions")

  jacoco
}

group = "io.opentelemetry.instrumentation"

dependencies {
  api("io.opentelemetry:opentelemetry-api")
}

tasks.test {
  finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
  dependsOn(tasks.test)
}
