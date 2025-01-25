plugins {
  id("otel.java-conventions")
}

dependencies {
  api(project(":testing-common"))

  implementation("com.github.briandilley.jsonrpc4j:jsonrpc4j:1.3.3")
  implementation("com.fasterxml.jackson.core:jackson-databind:2.13.3")
}
