plugins {
  id("otel.javaagent-instrumentation")
}

muzzle {
  pass {
    group.set("javax.ws.rs")
    module.set("javax.ws.rs-api")
    versions.set("[2.0,)")
  }
  pass {
    // We want to support the dropwizard clients too.
    group.set("io.dropwizard")
    module.set("dropwizard-client")
    versions.set("[0.8.0,)")
    assertInverse.set(true)
  }
}

dependencies {
  compileOnly("javax.ws.rs:javax.ws.rs-api:2.0.1")
  compileOnly("javax.annotation:javax.annotation-api:1.3.2")

  testInstrumentation(project(":instrumentation:jaxrs-client:jaxrs-client-2.0:jaxrs-client-2.0-cxf-3.0:javaagent"))
  testInstrumentation(project(":instrumentation:jaxrs-client:jaxrs-client-2.0:jaxrs-client-2.0-jersey-2.0:javaagent"))
  testInstrumentation(project(":instrumentation:jaxrs-client:jaxrs-client-2.0:jaxrs-client-2.0-resteasy-3.0:javaagent"))

  testImplementation("javax.ws.rs:javax.ws.rs-api:2.0.1")

  testLibrary("org.glassfish.jersey.core:jersey-client:2.0")
  testLibrary("org.jboss.resteasy:resteasy-client:3.0.5.Final")
  // ^ This version has timeouts https://issues.redhat.com/browse/RESTEASY-975
  testLibrary("org.apache.cxf:cxf-rt-rs-client:3.1.0")
  // Doesn't work with CXF 3.0.x because their context is wrong:
  // https://github.com/apache/cxf/commit/335c7bad2436f08d6d54180212df5a52157c9f21

  testImplementation("javax.xml.bind:jaxb-api:2.2.3")

  testInstrumentation(project(":instrumentation:apache-httpclient:apache-httpclient-4.0:javaagent"))

  latestDepTestLibrary("org.glassfish.jersey.inject:jersey-hk2:2.+")
  latestDepTestLibrary("org.glassfish.jersey.core:jersey-client:2.+")
  latestDepTestLibrary("org.jboss.resteasy:resteasy-client:3.0.26.Final")
}

// Requires old Guava. Can't use enforcedPlatform since predates BOM
configurations.testRuntimeClasspath.resolutionStrategy.force("com.google.guava:guava:19.0")
