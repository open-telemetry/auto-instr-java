plugins {
  id("otel.library-instrumentation")
  id("net.ltgt.nullaway")
}

dependencies {
  library("org.mongodb:mongo-java-driver:3.1.0")

  testImplementation(project(":instrumentation:mongo:mongo-3.1:testing"))
}