plugins {
  id("otel.javaagent-instrumentation")
}

muzzle {
  pass {
    group.set("org.elasticsearch.client")
    module.set("rest")
    versions.set("[5.0,6.4)")
    assertInverse.set(true)
  }

  pass {
    group.set("org.elasticsearch.client")
    module.set("elasticsearch-rest-client")
    versions.set("[5.0,6.4)")
  }
}

dependencies {
  compileOnly("org.elasticsearch.client:rest:5.0.0")

  implementation(project(":instrumentation:elasticsearch:elasticsearch-rest-common-5.0:javaagent"))

  testInstrumentation(project(":instrumentation:apache-httpclient:apache-httpclient-4.0:javaagent"))
  testInstrumentation(project(":instrumentation:apache-httpasyncclient-4.1:javaagent"))

  testImplementation("org.apache.logging.log4j:log4j-core:2.11.0")
  testImplementation("org.apache.logging.log4j:log4j-api:2.11.0")
  testImplementation("com.fasterxml.jackson.core:jackson-databind")

  testImplementation("org.testcontainers:elasticsearch")
  testLibrary("org.elasticsearch.client:rest:5.0.0")

  latestDepTestLibrary("org.elasticsearch.client:elasticsearch-rest-client:6.3.+") // see elasticsearch-rest-6.4 module
}

tasks {
  withType<Test>().configureEach {
    systemProperty("testLatestDeps", findProperty("testLatestDeps") as Boolean)
    usesService(gradle.sharedServices.registrations["testcontainersBuildService"].service)
  }
}
