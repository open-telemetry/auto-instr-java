plugins {
  id("otel.java-conventions")
}

dependencies {
  testImplementation(project(":instrumentation:elasticsearch:elasticsearch-rest-common-5.0:javaagent"))
  testImplementation(project(":instrumentation:elasticsearch:elasticsearch-api-client-7.16:javaagent"))
}
