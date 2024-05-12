plugins {
  id("otel.library-instrumentation")
  id("otel.nullaway-conventions")

  jacoco
}

dependencies {
  library("com.linecorp.armeria:armeria:1.3.0")

  testImplementation(project(":instrumentation:armeria-1.3:testing"))
}

tasks {
  withType<Test>().configureEach {
    systemProperty("testLatestDeps", findProperty("testLatestDeps") as Boolean)
  }
}

tasks.test {
  finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
  dependsOn(tasks.test)
}
