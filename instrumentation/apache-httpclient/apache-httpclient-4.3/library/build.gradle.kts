plugins {
  id("otel.library-instrumentation")
  id("otel.nullaway-conventions")
  id("otel.animalsniffer-conventions")

  jacoco
}

dependencies {
  library("org.apache.httpcomponents:httpclient:4.3")

  testImplementation(project(":instrumentation:apache-httpclient:apache-httpclient-4.3:testing"))

  latestDepTestLibrary("org.apache.httpcomponents:httpclient:4.+") // see apache-httpclient-5.0 module
}

tasks.test {
  finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
  dependsOn(tasks.test)
}
