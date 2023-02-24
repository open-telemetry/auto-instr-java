plugins {
  id("otel.javaagent-instrumentation")
}

muzzle {
  fail {
    group.set("commons-httpclient")
    module.set("commons-httpclient")
    versions.set("[,4.0)")
  }
  pass {
    group.set("org.apache.httpcomponents")
    module.set("httpclient")
    versions.set("[4.0,)")
    assertInverse.set(true)
  }
  pass {
    // We want to support the dropwizard clients too.
    group.set("io.dropwizard")
    module.set("dropwizard-client")
    versions.set("(,)")
    assertInverse.set(true)
  }
}

dependencies {
  implementation(project(":instrumentation:apache-httpclient:commons:javaagent"))
  implementation(project(":instrumentation:apache-httpclient:commons-4.0:javaagent"))
  // 4.0.x uses GuardedBy which interferes with compiling tests
  library("org.apache.httpcomponents:httpclient:4.1")
}
