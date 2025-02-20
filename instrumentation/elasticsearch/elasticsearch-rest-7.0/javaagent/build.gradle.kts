plugins {
  id("otel.javaagent-instrumentation")
}

muzzle {
  pass {
    group.set("org.elasticsearch.client")
    module.set("elasticsearch-rest-client")
    versions.set("[7.0,)")
    assertInverse.set(true)
  }

  fail {
    group.set("org.elasticsearch.client")
    module.set("elasticsearch-rest-client")
    versions.set("[8.10,)")
    // elasticsearch-java 8.10+ has native, on-by-default opentelemetry instrumentation
    // we disable our elasticsearch-rest-client instrumentation when elasticsearch-java is present
    extraDependency("co.elastic.clients:elasticsearch-java:8.10.0")
  }

  fail {
    group.set("org.elasticsearch.client")
    module.set("rest")
    versions.set("(,)")
  }
}

dependencies {
  library("org.elasticsearch.client:elasticsearch-rest-client:7.0.0")

  implementation(project(":instrumentation:elasticsearch:elasticsearch-rest-common-5.0:javaagent"))

  testInstrumentation(project(":instrumentation:apache-httpclient:apache-httpclient-4.0:javaagent"))
  testInstrumentation(project(":instrumentation:apache-httpasyncclient-4.1:javaagent"))

  testImplementation("com.fasterxml.jackson.core:jackson-databind")
  testImplementation("org.testcontainers:elasticsearch")
}

tasks {
  test {
    usesService(gradle.sharedServices.registrations["testcontainersBuildService"].service)
  }
}
