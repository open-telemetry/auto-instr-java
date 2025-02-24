plugins {
  id("otel.library-instrumentation")
}

dependencies {
  library("com.github.briandilley.jsonrpc4j:jsonrpc4j:1.3.3")
  library("com.fasterxml.jackson.core:jackson-databind")

  testImplementation(project(":instrumentation:jsonrpc4j-1.3:testing"))
}
