plugins {
  id("otel.java-conventions")
}

dependencies {
  api(project(":testing-common"))

  implementation("com.datastax.oss:java-driver-core:4.4.0")
  implementation("io.projectreactor:reactor-core:3.5.3")

  api(project(":instrumentation:cassandra:cassandra-4.0-common:testing"))
}
