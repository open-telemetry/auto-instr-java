plugins {
  id("otel.library-instrumentation")
}

dependencies {
  library("org.elasticsearch.client:elasticsearch-rest-client:7.0.0")
  implementation("net.bytebuddy:byte-buddy")
  implementation(project(":instrumentation:elasticsearch:elasticsearch-rest-common-5.0:library"))

  testImplementation("com.fasterxml.jackson.core:jackson-databind")
  testImplementation("org.testcontainers:elasticsearch")
}

tasks {
  test {
    usesService(gradle.sharedServices.registrations["testcontainersBuildService"].service)
  }
}
