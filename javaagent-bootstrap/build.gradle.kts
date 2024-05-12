plugins {
  id("otel.java-conventions")
  id("otel.publish-conventions")

  jacoco
}

group = "io.opentelemetry.javaagent"

dependencies {
  implementation(project(":instrumentation-api"))

  testImplementation(project(":testing-common"))
}

tasks.withType<Test>().configureEach {
  // required on jdk17
  jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
  jvmArgs("-XX:+IgnoreUnrecognizedVMOptions")
}

tasks.test {
  finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
  dependsOn(tasks.test)
}
