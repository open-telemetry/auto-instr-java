plugins {
  id("otel.javaagent-instrumentation")
}

muzzle {
  pass {
    group.set("com.github.briandilley.jsonrpc4j")
    module.set("jsonrpc4j")
    versions.set("[1.3.3,)")
    assertInverse.set(true)
  }
}

dependencies {
  implementation(project(":instrumentation:jsonrpc4j-1.3:library"))

  library("com.github.briandilley.jsonrpc4j:jsonrpc4j:1.3.3")

  testImplementation(project(":instrumentation:jsonrpc4j-1.3:testing"))
  testImplementation("com.fasterxml.jackson.core:jackson-databind:2.13.3")
  testImplementation("org.eclipse.jetty:jetty-server:9.4.49.v20220914")
  testImplementation("org.eclipse.jetty:jetty-servlet:9.4.49.v20220914")
  testImplementation("javax.portlet:portlet-api:2.0")
}
